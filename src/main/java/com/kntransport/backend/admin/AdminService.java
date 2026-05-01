package com.kntransport.backend.admin;

import com.kntransport.backend.dto.*;
import com.kntransport.backend.entity.*;
import com.kntransport.backend.exception.BadRequestException;
import com.kntransport.backend.exception.ResourceNotFoundException;
import com.kntransport.backend.repository.*;
import com.kntransport.backend.service.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository               userRepository;
    private final TripBookingRepository        tripRepository;
    private final LiftClubRepository           liftClubRepository;
    private final QuoteRepository              quoteRepository;
    private final LiftClubSubscriptionRepository subscriptionRepository;
    private final VehicleRepository            vehicleRepository;
    private final PasswordEncoder              passwordEncoder;
    private final StorageService               storageService;

    public AdminService(UserRepository userRepository,
                        TripBookingRepository tripRepository,
                        LiftClubRepository liftClubRepository,
                        QuoteRepository quoteRepository,
                        LiftClubSubscriptionRepository subscriptionRepository,
                        VehicleRepository vehicleRepository,
                        PasswordEncoder passwordEncoder,
                        StorageService storageService) {
        this.userRepository        = userRepository;
        this.tripRepository        = tripRepository;
        this.liftClubRepository    = liftClubRepository;
        this.quoteRepository       = quoteRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.vehicleRepository     = vehicleRepository;
        this.passwordEncoder       = passwordEncoder;
        this.storageService        = storageService;
    }

    // ── User management ───────────────────────────────────────────────────────

    public Page<UserDto> listUsers(String roleFilter, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        if (roleFilter != null && !roleFilter.isBlank()) {
            User.Role role = User.Role.valueOf(roleFilter.toUpperCase());
            return userRepository.findByRoleOrderByNameAsc(role, pr).map(UserDto::from);
        }
        return userRepository.findAllByOrderByNameAsc(pr).map(UserDto::from);
    }

    public UserDto getUser(String id) {
        return UserDto.from(findUser(id));
    }

    @Transactional
    public UserDto createUser(AdminUserRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BadRequestException("Email already registered: " + req.email());
        }
        if (req.password() == null || req.password().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPhone(req.phone());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole(User.Role.valueOf(req.role().toUpperCase()));
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public UserDto updateUser(String id, AdminUserRequest req) {
        User user = findUser(id);

        if (!user.getEmail().equals(req.email()) && userRepository.existsByEmail(req.email())) {
            throw new BadRequestException("Email already in use: " + req.email());
        }

        user.setName(req.name());
        user.setEmail(req.email());
        user.setPhone(req.phone());
        user.setRole(User.Role.valueOf(req.role().toUpperCase()));

        if (req.password() != null && req.password().length() >= 6) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }

        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(String id) {
        User user = findUser(id);
        if (user.getRole() == User.Role.ADMIN) {
            throw new BadRequestException("Cannot delete an admin account");
        }
        userRepository.delete(user);
    }

    // ── Analytics dashboard ───────────────────────────────────────────────────

    public AnalyticsDto getAnalytics() {
        long totalUsers      = userRepository.count();
        long totalCommuters  = userRepository.countByRole(User.Role.COMMUTER);
        long totalDrivers    = userRepository.countByRole(User.Role.DRIVER);

        long totalTrips      = tripRepository.count();
        long pendingQuote    = tripRepository.countByStatus(TripBooking.TripStatus.PENDING_QUOTE);
        long confirmed       = tripRepository.countByStatus(TripBooking.TripStatus.CONFIRMED) +
                               tripRepository.countByStatus(TripBooking.TripStatus.QUOTE_ACCEPTED) +
                               tripRepository.countByStatus(TripBooking.TripStatus.IN_PROGRESS);
        long completed       = tripRepository.countByStatus(TripBooking.TripStatus.COMPLETED);
        long cancelled       = tripRepository.countByStatus(TripBooking.TripStatus.CANCELLED);

        long totalLiftClubs  = liftClubRepository.count();
        long openLiftClubs   = liftClubRepository.countByStatus(LiftClub.LiftClubStatus.OPEN);
        long activeLiftClubs = liftClubRepository.countByStatus(LiftClub.LiftClubStatus.ACTIVE);

        double totalRevenue      = tripRepository.sumCompletedRevenue();
        double totalOutstanding  = tripRepository.sumOutstandingRevenue();
        double avgTripValue      = completed > 0 ? totalRevenue / completed : 0.0;

        // Revenue by month (last 12 months)
        List<Object[]> rawMonths = tripRepository.revenueByMonth(PageRequest.of(0, 12));
        List<AnalyticsDto.MonthlyRevenue> revenueByMonth = rawMonths.stream()
                .map(row -> new AnalyticsDto.MonthlyRevenue(
                        (String) row[0],
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).longValue()))
                .collect(Collectors.toList());

        // Trip status breakdown
        List<AnalyticsDto.TripStatusBreakdown> breakdown = new ArrayList<>();
        if (totalTrips > 0) {
            Map<String, Long> counts = Map.of(
                "PENDING_QUOTE", pendingQuote,
                "CONFIRMED",     confirmed,
                "COMPLETED",     completed,
                "CANCELLED",     cancelled
            );
            counts.forEach((status, count) ->
                breakdown.add(new AnalyticsDto.TripStatusBreakdown(
                    status, count, Math.round(count * 1000.0 / totalTrips) / 10.0)));
        }

        return new AnalyticsDto(
                totalUsers, totalCommuters, totalDrivers,
                totalTrips, pendingQuote, confirmed, completed, cancelled,
                totalLiftClubs, openLiftClubs, activeLiftClubs,
                totalRevenue, totalOutstanding, avgTripValue,
                revenueByMonth, breakdown);
    }

    // ── Financial report ──────────────────────────────────────────────────────

    public FinancialReportDto generateFinancialReport(String from, String to, String generatedByEmail) {
        List<Quote> allQuotes = quoteRepository.findAllDecided();

        // Build line items
        List<FinancialReportDto.LineItem> lineItems = new ArrayList<>();
        double grossRevenue       = 0;
        double onceOffRevenue     = 0;
        double weeklyRevenue      = 0;
        double fortnightlyRevenue = 0;
        double monthlyRevenue     = 0;

        for (Quote q : allQuotes) {
            String clientName  = "";
            String clientPhone = "";
            String date        = "";

            if (q.getReferenceType() == Quote.ReferenceType.TRIP) {
                Optional<TripBooking> trip = tripRepository.findById(q.getReferenceId());
                if (trip.isPresent()) {
                    clientName  = trip.get().getCommuter().getName();
                    clientPhone = trip.get().getCommuter().getPhone();
                    date        = trip.get().getDate().toString();
                }
            } else {
                Optional<LiftClub> lc = liftClubRepository.findById(q.getReferenceId());
                if (lc.isPresent()) {
                    clientName  = lc.get().getCreator().getName();
                    clientPhone = lc.get().getCreator().getPhone();
                    date        = lc.get().getDepartureTime().toString();
                }
            }

            String cycle = q.getPaymentCycle() != null ? q.getPaymentCycle().name() : "ONCE_OFF";

            lineItems.add(new FinancialReportDto.LineItem(
                    date,
                    q.getReferenceType().name(),
                    q.getReferenceId().toString(),
                    clientName,
                    clientPhone,
                    cycle,
                    q.getAmount(),
                    Boolean.TRUE.equals(q.getAccepted())
            ));

            if (Boolean.TRUE.equals(q.getAccepted())) {
                grossRevenue += q.getAmount();
                switch (cycle) {
                    case "WEEKLY"       -> weeklyRevenue      += q.getAmount();
                    case "FORTNIGHTLY"  -> fortnightlyRevenue += q.getAmount();
                    case "MONTHLY"      -> monthlyRevenue     += q.getAmount();
                    default             -> onceOffRevenue     += q.getAmount();
                }
            }
        }

        long   invoiceCount    = lineItems.stream().filter(FinancialReportDto.LineItem::accepted).count();
        double avgInvoice      = invoiceCount > 0 ? grossRevenue / invoiceCount : 0;
        double highestInvoice  = lineItems.stream().filter(FinancialReportDto.LineItem::accepted)
                                         .mapToDouble(FinancialReportDto.LineItem::amount).max().orElse(0);
        double lowestInvoice   = lineItems.stream().filter(FinancialReportDto.LineItem::accepted)
                                         .mapToDouble(FinancialReportDto.LineItem::amount).min().orElse(0);

        // Sort line items newest first
        lineItems.sort(Comparator.comparing(FinancialReportDto.LineItem::date).reversed());

        return new FinancialReportDto(
                from, to,
                Instant.now().toString(),
                generatedByEmail,
                grossRevenue, invoiceCount, avgInvoice, highestInvoice, lowestInvoice,
                onceOffRevenue, weeklyRevenue, fortnightlyRevenue, monthlyRevenue,
                lineItems);
    }

    // ── Vehicle fleet management ──────────────────────────────────────────────

    public Page<VehicleDto> listVehicles(Boolean activeOnly, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        Page<Vehicle> vehicles = Boolean.TRUE.equals(activeOnly)
                ? vehicleRepository.findByActiveOrderByMakeAscModelAsc(true, pr)
                : vehicleRepository.findAllByOrderByMakeAscModelAsc(pr);

        // Build a lookup: vehicleId → assigned driver
        Map<UUID, User> vehicleToDriver = new HashMap<>();
        userRepository.findAll().forEach(u -> {
            if (u.getCurrentVehicle() != null) {
                vehicleToDriver.put(u.getCurrentVehicle().getId(), u);
            }
        });

        return vehicles.map(v -> {
            User d = vehicleToDriver.get(v.getId());
            return VehicleDto.from(v,
                    d != null ? d.getId().toString() : null,
                    d != null ? d.getName()          : null);
        });
    }

    public VehicleDto getVehicle(String id) {
        Vehicle v = findVehicle(id);
        User d = userRepository.findAll().stream()
                .filter(u -> u.getCurrentVehicle() != null && u.getCurrentVehicle().getId().equals(v.getId()))
                .findFirst().orElse(null);
        return VehicleDto.from(v, d != null ? d.getId().toString() : null, d != null ? d.getName() : null);
    }

    @Transactional
    public VehicleDto createVehicle(VehicleRequest req) {
        if (vehicleRepository.existsByPlate(req.plate())) {
            throw new BadRequestException("Vehicle with plate " + req.plate() + " already exists");
        }
        Vehicle v = new Vehicle();
        applyVehicleFields(v, req);
        return VehicleDto.from(vehicleRepository.save(v), null, null);
    }

    @Transactional
    public VehicleDto updateVehicle(String id, VehicleRequest req) {
        Vehicle v = findVehicle(id);
        if (!v.getPlate().equals(req.plate()) && vehicleRepository.existsByPlate(req.plate())) {
            throw new BadRequestException("Plate " + req.plate() + " already in use");
        }
        applyVehicleFields(v, req);
        Vehicle saved = vehicleRepository.save(v);
        User d = userRepository.findAll().stream()
                .filter(u -> u.getCurrentVehicle() != null && u.getCurrentVehicle().getId().equals(saved.getId()))
                .findFirst().orElse(null);
        return VehicleDto.from(saved, d != null ? d.getId().toString() : null, d != null ? d.getName() : null);
    }

    @Transactional
    public void deactivateVehicle(String id) {
        Vehicle v = findVehicle(id);
        v.setActive(false);
        vehicleRepository.save(v);
        // Unassign from any driver who currently has this vehicle
        userRepository.findAll().stream()
                .filter(u -> u.getCurrentVehicle() != null && u.getCurrentVehicle().getId().equals(v.getId()))
                .forEach(u -> { u.setCurrentVehicle(null); userRepository.save(u); });
    }

    public VehicleDto uploadVehiclePhoto(String id, MultipartFile file) throws IOException {
        String url = storageService.store("vehicles", file);
        Vehicle v = findVehicle(id);
        v.setPhotoUrl(url);
        return VehicleDto.from(vehicleRepository.save(v), null, null);
    }

    @Transactional
    public VehicleDto reactivateVehicle(String id) {
        Vehicle v = findVehicle(id);
        v.setActive(true);
        return VehicleDto.from(vehicleRepository.save(v), null, null);
    }

    /**
     * Assign a vehicle to a driver.
     * vehicleId null = unassign the driver's current vehicle.
     */
    @Transactional
    public UserDto assignVehicleToDriver(String driverId, AssignVehicleRequest req) {
        User driver = findUser(driverId);
        if (driver.getRole() != User.Role.DRIVER) {
            throw new BadRequestException("User is not a driver");
        }
        if (req.vehicleId() == null || req.vehicleId().isBlank()) {
            driver.setCurrentVehicle(null);
        } else {
            Vehicle v = findVehicle(req.vehicleId());
            if (!v.isActive()) {
                throw new BadRequestException("Vehicle is not active");
            }
            // Unassign from any other driver who already has this vehicle
            userRepository.findAll().stream()
                    .filter(u -> !u.getId().equals(driver.getId())
                              && u.getCurrentVehicle() != null
                              && u.getCurrentVehicle().getId().equals(v.getId()))
                    .forEach(u -> { u.setCurrentVehicle(null); userRepository.save(u); });
            driver.setCurrentVehicle(v);
        }
        return UserDto.from(userRepository.save(driver));
    }

    private void applyVehicleFields(Vehicle v, VehicleRequest req) {
        v.setMake(req.make());
        v.setModel(req.model());
        v.setColour(req.colour());
        v.setPlate(req.plate().toUpperCase());
        v.setYear(req.year());
        v.setVehicleType(req.vehicleType() != null
                ? Vehicle.VehicleType.valueOf(req.vehicleType().toUpperCase())
                : Vehicle.VehicleType.MINIBUS);
        v.setPhotoUrl(req.photoUrl());
        v.setNotes(req.notes());
        v.setActive(true);
    }

    private Vehicle findVehicle(String id) {
        return vehicleRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
    }


    // ── Trip management ───────────────────────────────────────────────────────

    /** All trips across all commuters (admin view). */
    public PagedResponse<TripBookingDto> listAllTrips(int page, int size) {
        return PagedResponse.from(
                tripRepository.findAll(PageRequest.of(page, size)),
                TripBookingDto::from);
    }

    /** Assign a driver to a trip and update driverName for display. */
    @Transactional
    public TripBookingDto assignDriver(String tripId, AssignDriverRequest req) {
        TripBooking trip = tripRepository.findById(UUID.fromString(tripId))
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));

        User driver = userRepository.findById(UUID.fromString(req.driverId()))
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + req.driverId()));

        if (driver.getRole() != User.Role.DRIVER) {
            throw new BadRequestException("User is not a driver: " + req.driverId());
        }

        trip.setDriver(driver);
        trip.setDriverName(driver.getName());
        // Snapshot the driver's current vehicle onto the trip
        if (driver.getCurrentVehicle() != null) {
            trip.setVehicle(driver.getCurrentVehicle());
            trip.setVehicleInfo(driver.getCurrentVehicle().getMake() + " "
                    + driver.getCurrentVehicle().getModel() + " — "
                    + driver.getCurrentVehicle().getColour());
            trip.setVehiclePlate(driver.getCurrentVehicle().getPlate());
        }
        if (trip.getStatus() == TripBooking.TripStatus.PENDING_QUOTE ||
            trip.getStatus() == TripBooking.TripStatus.QUOTE_ACCEPTED) {
            trip.setStatus(TripBooking.TripStatus.CONFIRMED);
        }
        return TripBookingDto.from(tripRepository.save(trip));
    }

    /** Admin cancels any trip regardless of status. */
    @Transactional
    public TripBookingDto cancelTrip(String tripId, CancelTripRequest req) {
        TripBooking trip = tripRepository.findById(UUID.fromString(tripId))
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));
        if (trip.getStatus() == TripBooking.TripStatus.COMPLETED ||
            trip.getStatus() == TripBooking.TripStatus.CANCELLED) {
            throw new BadRequestException("Trip is already " + trip.getStatus().name().toLowerCase());
        }
        trip.setStatus(TripBooking.TripStatus.CANCELLED);
        return TripBookingDto.from(tripRepository.save(trip));
    }

    /** Admin updates a trip's quoted amount. */
    @Transactional
    public TripBookingDto updateQuote(String tripId, double amount) {
        TripBooking trip = tripRepository.findById(UUID.fromString(tripId))
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));
        trip.setQuotedAmount(amount);
        return TripBookingDto.from(tripRepository.save(trip));
    }

    /** Admin fetches a single trip by ID. */
    public TripBookingDto getTrip(String tripId) {
        return TripBookingDto.from(
                tripRepository.findById(UUID.fromString(tripId))
                        .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId))
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findUser(String id) {
        return userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
