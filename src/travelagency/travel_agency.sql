-- =====================================================================
--  Travel Agency Management System - MySQL database
--  Database Systems project: Hafsa Khurram (FA22-BCS-058),
--  Zainab Asif (FA22-BCS-068), Syeda Humaima (FA22-BCS-066)
--
--  Run this whole file in MySQL Workbench (or let the app run it for you
--  the first time it starts). It is safe to run more than once.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS travel_agency
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travel_agency;

-- ---------------------------------------------------------------------
--  People
-- ---------------------------------------------------------------------

-- Admins manage packages, routes, hotels, employees and bookings.
CREATE TABLE IF NOT EXISTS admins (
    admin_id      INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(30)  NOT NULL UNIQUE,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash CHAR(64)     NOT NULL,          -- SHA-256 of (salt + password)
    salt          CHAR(32)     NOT NULL,
    role          ENUM('Super Admin', 'Admin') NOT NULL DEFAULT 'Admin',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Customers sign up in the app and make bookings.
CREATE TABLE IF NOT EXISTS customers (
    customer_id   INT AUTO_INCREMENT PRIMARY KEY,
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    phone         VARCHAR(20)  NOT NULL,
    gender        ENUM('Male', 'Female', 'Other') NOT NULL,
    address       VARCHAR(255),
    username      VARCHAR(30)  NOT NULL UNIQUE,
    password_hash CHAR(64)     NOT NULL,
    salt          CHAR(32)     NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Agency staff, managed by admins.
CREATE TABLE IF NOT EXISTS employees (
    employee_id INT AUTO_INCREMENT PRIMARY KEY,
    first_name  VARCHAR(50)   NOT NULL,
    last_name   VARCHAR(50)   NOT NULL,
    email       VARCHAR(150)  NOT NULL UNIQUE,
    phone       VARCHAR(20)   NOT NULL,
    job_title   VARCHAR(100)  NOT NULL,
    department  VARCHAR(100)  NOT NULL,
    hire_date   DATE          NOT NULL,
    salary      DECIMAL(10,2) NOT NULL,
    CONSTRAINT chk_employee_salary CHECK (salary >= 0)
);

-- ---------------------------------------------------------------------
--  Catalogue: what customers can book
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS travel_packages (
    package_id         INT AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(100)  NOT NULL,
    package_type       ENUM('Economy', 'Standard', 'Luxury', 'Adventure', 'Family', 'Honeymoon') NOT NULL,
    destination        VARCHAR(100)  NOT NULL,
    description        TEXT,
    start_date         DATE          NOT NULL,
    end_date           DATE          NOT NULL,
    price_per_person   DECIMAL(10,2) NOT NULL,
    total_seats        INT           NOT NULL,
    flight_included    BOOLEAN       NOT NULL DEFAULT FALSE,
    hotel_included     BOOLEAN       NOT NULL DEFAULT FALSE,
    transport_included BOOLEAN       NOT NULL DEFAULT FALSE,
    meal_plan          ENUM('None', 'Breakfast', 'Half Board', 'Full Board') NOT NULL DEFAULT 'None',
    is_active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by         INT NULL,
    CONSTRAINT fk_package_admin FOREIGN KEY (created_by) REFERENCES admins(admin_id) ON DELETE SET NULL,
    CONSTRAINT chk_package_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_package_price CHECK (price_per_person > 0),
    CONSTRAINT chk_package_seats CHECK (total_seats > 0)
);

CREATE TABLE IF NOT EXISTS bus_routes (
    route_id         INT AUTO_INCREMENT PRIMARY KEY,
    departure_city   VARCHAR(60)   NOT NULL,
    destination_city VARCHAR(60)   NOT NULL,
    bus_company      VARCHAR(80)   NOT NULL,
    departure_times  VARCHAR(255)  NOT NULL,      -- e.g. "08:00;14:00;20:00" (24-hour)
    economy_seats    INT           NOT NULL,
    business_seats   INT           NOT NULL,
    economy_fare     DECIMAL(10,2) NOT NULL,
    business_fare    DECIMAL(10,2) NOT NULL,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_route UNIQUE (departure_city, destination_city),
    CONSTRAINT chk_route_cities CHECK (departure_city <> destination_city),
    CONSTRAINT chk_route_seats CHECK (economy_seats >= 0 AND business_seats >= 0 AND economy_seats + business_seats > 0),
    CONSTRAINT chk_route_fares CHECK (economy_fare > 0 AND business_fare > 0)
);

CREATE TABLE IF NOT EXISTS hotels (
    hotel_id     INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    city         VARCHAR(60)   NOT NULL,
    address      VARCHAR(255),
    star_rating  TINYINT       NOT NULL,
    single_rooms INT           NOT NULL,
    double_rooms INT           NOT NULL,
    suite_rooms  INT           NOT NULL,
    single_rate  DECIMAL(10,2) NOT NULL,          -- price per room per night
    double_rate  DECIMAL(10,2) NOT NULL,
    suite_rate   DECIMAL(10,2) NOT NULL,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_hotel UNIQUE (name, city),
    CONSTRAINT chk_hotel_stars CHECK (star_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_hotel_rooms CHECK (single_rooms >= 0 AND double_rooms >= 0 AND suite_rooms >= 0),
    CONSTRAINT chk_hotel_rates CHECK (single_rate > 0 AND double_rate > 0 AND suite_rate > 0)
);

-- Meal plan price per room per night (used for hotel bookings).
CREATE TABLE IF NOT EXISTS meal_plans (
    plan            VARCHAR(20)   PRIMARY KEY,
    price_per_night DECIMAL(10,2) NOT NULL,
    CONSTRAINT chk_meal_price CHECK (price_per_night >= 0)
);

-- ---------------------------------------------------------------------
--  Bookings (generalization: one row in "bookings" + one detail row
--  in package_bookings, bus_bookings or hotel_bookings)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS bookings (
    booking_id   INT AUTO_INCREMENT PRIMARY KEY,
    customer_id  INT           NOT NULL,
    booking_type ENUM('Package', 'Bus', 'Hotel') NOT NULL,
    status       ENUM('Pending', 'Approved', 'Cancelled') NOT NULL DEFAULT 'Pending',
    travel_date  DATE          NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    handled_by   INT NULL,                        -- admin who approved / cancelled
    booked_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_admin FOREIGN KEY (handled_by) REFERENCES admins(admin_id) ON DELETE SET NULL,
    CONSTRAINT chk_booking_amount CHECK (total_amount > 0),
    INDEX idx_booking_status (status),
    INDEX idx_booking_type_date (booking_type, travel_date)
);

CREATE TABLE IF NOT EXISTS package_bookings (
    booking_id INT PRIMARY KEY,
    package_id INT NOT NULL,
    travellers INT NOT NULL,
    CONSTRAINT fk_pb_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_pb_package FOREIGN KEY (package_id) REFERENCES travel_packages(package_id),
    CONSTRAINT chk_pb_travellers CHECK (travellers > 0)
);

CREATE TABLE IF NOT EXISTS bus_bookings (
    booking_id     INT PRIMARY KEY,
    route_id       INT NOT NULL,
    departure_time CHAR(5) NOT NULL,              -- "HH:MM"
    seat_class     ENUM('Economy', 'Business') NOT NULL,
    seats          INT NOT NULL,
    CONSTRAINT fk_bb_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_bb_route FOREIGN KEY (route_id) REFERENCES bus_routes(route_id),
    CONSTRAINT chk_bb_seats CHECK (seats > 0)
);

CREATE TABLE IF NOT EXISTS hotel_bookings (
    booking_id       INT PRIMARY KEY,
    hotel_id         INT NOT NULL,
    room_type        ENUM('Single', 'Double', 'Suite') NOT NULL,
    rooms            INT NOT NULL,
    guests           INT NOT NULL,
    check_in         DATE NOT NULL,
    check_out        DATE NOT NULL,
    meal_plan        VARCHAR(20) NOT NULL,
    special_requests VARCHAR(255),
    CONSTRAINT fk_hb_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_hb_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id),
    CONSTRAINT fk_hb_meal FOREIGN KEY (meal_plan) REFERENCES meal_plans(plan),
    CONSTRAINT chk_hb_rooms CHECK (rooms > 0 AND guests > 0),
    CONSTRAINT chk_hb_dates CHECK (check_out > check_in),
    INDEX idx_hb_dates (hotel_id, check_in, check_out)
);

-- One payment per booking.
CREATE TABLE IF NOT EXISTS payments (
    payment_id  INT AUTO_INCREMENT PRIMARY KEY,
    booking_id  INT           NOT NULL UNIQUE,
    amount      DECIMAL(10,2) NOT NULL,
    method      ENUM('Cash', 'Card', 'Bank Transfer', 'JazzCash', 'EasyPaisa') NOT NULL,
    status      ENUM('Paid', 'Refunded') NOT NULL DEFAULT 'Paid',
    paid_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refunded_at TIMESTAMP     NULL,
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

-- One feedback per booking.
CREATE TABLE IF NOT EXISTS feedback (
    feedback_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id  INT     NOT NULL UNIQUE,
    customer_id INT     NOT NULL,
    rating      TINYINT NOT NULL,
    comments    VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT chk_feedback_rating CHECK (rating BETWEEN 1 AND 5)
);

-- ---------------------------------------------------------------------
--  Trigger: cancelling a booking refunds its payment automatically
-- ---------------------------------------------------------------------

DROP TRIGGER IF EXISTS trg_refund_on_cancel;
DELIMITER $$
CREATE TRIGGER trg_refund_on_cancel
AFTER UPDATE ON bookings
FOR EACH ROW
BEGIN
    IF NEW.status = 'Cancelled' AND OLD.status <> 'Cancelled' THEN
        UPDATE payments
           SET status = 'Refunded', refunded_at = CURRENT_TIMESTAMP
         WHERE booking_id = NEW.booking_id AND status = 'Paid';
    END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
--  Function: seats still free on a travel package
-- ---------------------------------------------------------------------

DROP FUNCTION IF EXISTS fn_package_seats_left;
DELIMITER $$
CREATE FUNCTION fn_package_seats_left(p_package_id INT)
RETURNS INT
READS SQL DATA
BEGIN
    DECLARE booked INT;
    SELECT COALESCE(SUM(pb.travellers), 0) INTO booked
      FROM package_bookings pb
      JOIN bookings b ON b.booking_id = pb.booking_id
     WHERE pb.package_id = p_package_id AND b.status <> 'Cancelled';
    RETURN (SELECT total_seats FROM travel_packages WHERE package_id = p_package_id) - booked;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
--  Views used by the reports
-- ---------------------------------------------------------------------

CREATE OR REPLACE VIEW v_booking_overview AS
SELECT b.booking_id,
       b.customer_id,
       CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
       b.booking_type,
       CASE b.booking_type
           WHEN 'Package' THEN CONCAT(tp.name, ' (', pb.travellers, ' traveller(s))')
           WHEN 'Bus'     THEN CONCAT(br.departure_city, ' to ', br.destination_city, ', ', bb.departure_time,
                                      ', ', bb.seats, ' ', bb.seat_class, ' seat(s)')
           ELSE                CONCAT(h.name, ', ', h.city, ': ', hb.rooms, ' ', hb.room_type, ' room(s), ',
                                      DATEDIFF(hb.check_out, hb.check_in), ' night(s)')
       END AS details,
       b.travel_date,
       b.total_amount,
       b.status,
       p.method  AS payment_method,
       p.status  AS payment_status,
       b.booked_at
  FROM bookings b
  JOIN customers c             ON c.customer_id = b.customer_id
  LEFT JOIN package_bookings pb ON pb.booking_id = b.booking_id
  LEFT JOIN travel_packages tp  ON tp.package_id = pb.package_id
  LEFT JOIN bus_bookings bb     ON bb.booking_id = b.booking_id
  LEFT JOIN bus_routes br       ON br.route_id = bb.route_id
  LEFT JOIN hotel_bookings hb   ON hb.booking_id = b.booking_id
  LEFT JOIN hotels h            ON h.hotel_id = hb.hotel_id
  LEFT JOIN payments p          ON p.booking_id = b.booking_id;

CREATE OR REPLACE VIEW v_revenue_by_type AS
SELECT b.booking_type,
       COUNT(*)            AS bookings,
       SUM(b.total_amount) AS revenue
  FROM bookings b
 WHERE b.status <> 'Cancelled'
 GROUP BY b.booking_type;

-- ---------------------------------------------------------------------
--  Starting data (only inserted when the tables are empty)
-- ---------------------------------------------------------------------

-- Default admin:    username admin   / password admin123
-- Demo customer:    username demo    / password demo123
INSERT INTO admins (username, full_name, email, password_hash, salt, role)
SELECT 'admin', 'System Administrator', 'admin@travelagency.pk',
       SHA2(CONCAT('0f1e2d3c4b5a69788796a5b4c3d2e1f0', 'admin123'), 256),
       '0f1e2d3c4b5a69788796a5b4c3d2e1f0', 'Super Admin'
 WHERE NOT EXISTS (SELECT 1 FROM admins);

INSERT INTO customers (first_name, last_name, email, phone, gender, address, username, password_hash, salt)
SELECT 'Demo', 'Customer', 'demo@travelagency.pk', '0300-1234567', 'Female', 'Islamabad', 'demo',
       SHA2(CONCAT('a1b2c3d4e5f60718293a4b5c6d7e8f90', 'demo123'), 256), 'a1b2c3d4e5f60718293a4b5c6d7e8f90'
 WHERE NOT EXISTS (SELECT 1 FROM customers);

INSERT INTO meal_plans (plan, price_per_night)
SELECT * FROM (SELECT 'None' AS plan, 0 AS price UNION ALL SELECT 'Breakfast', 1500
               UNION ALL SELECT 'Half Board', 3500 UNION ALL SELECT 'Full Board', 5500) AS m
 WHERE NOT EXISTS (SELECT 1 FROM meal_plans);

INSERT INTO travel_packages (name, package_type, destination, description, start_date, end_date, price_per_person,
                             total_seats, flight_included, hotel_included, transport_included, meal_plan, created_by)
SELECT * FROM (
    SELECT 'Hunza Valley Escape' AS name, 'Adventure' AS type, 'Hunza' AS destination,
           'Karimabad, Attabad Lake, Passu Cones and Khunjerab Pass.' AS description,
           DATE_ADD(CURDATE(), INTERVAL 20 DAY) AS start_date, DATE_ADD(CURDATE(), INTERVAL 26 DAY) AS end_date,
           65000 AS price, 25 AS seats, FALSE AS flight, TRUE AS hotel, TRUE AS transport, 'Half Board' AS meal, 1 AS admin
    UNION ALL SELECT 'Skardu & Deosai Plains', 'Adventure', 'Skardu',
           'Shangrila Resort, Upper Kachura Lake, Deosai National Park and Shigar Fort.',
           DATE_ADD(CURDATE(), INTERVAL 35 DAY), DATE_ADD(CURDATE(), INTERVAL 41 DAY), 72000, 20, TRUE, TRUE, TRUE, 'Full Board', 1
    UNION ALL SELECT 'Swat Family Getaway', 'Family', 'Swat',
           'Malam Jabba, Kalam, Mahodand Lake and the Swat river.',
           DATE_ADD(CURDATE(), INTERVAL 12 DAY), DATE_ADD(CURDATE(), INTERVAL 16 DAY), 38000, 30, FALSE, TRUE, TRUE, 'Breakfast', 1
    UNION ALL SELECT 'Murree & Nathia Gali Weekend', 'Economy', 'Murree',
           'Mall Road, Patriata chair lift and the Nathia Gali pipeline track.',
           DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 9 DAY), 18000, 40, FALSE, TRUE, TRUE, 'Breakfast', 1
    UNION ALL SELECT 'Dubai City Lights', 'Luxury', 'Dubai',
           'Burj Khalifa, desert safari, Dubai Mall and a dhow cruise dinner.',
           DATE_ADD(CURDATE(), INTERVAL 45 DAY), DATE_ADD(CURDATE(), INTERVAL 50 DAY), 185000, 15, TRUE, TRUE, TRUE, 'Breakfast', 1
    UNION ALL SELECT 'Istanbul Honeymoon', 'Honeymoon', 'Istanbul',
           'Bosphorus cruise, Hagia Sophia, Cappadocia balloon ride and Turkish bath.',
           DATE_ADD(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 67 DAY), 320000, 10, TRUE, TRUE, TRUE, 'Half Board', 1
) AS p
 WHERE NOT EXISTS (SELECT 1 FROM travel_packages);

INSERT INTO bus_routes (departure_city, destination_city, bus_company, departure_times, economy_seats, business_seats,
                        economy_fare, business_fare)
SELECT * FROM (
    SELECT 'Lahore' AS f, 'Islamabad' AS t, 'Daewoo Express' AS bus, '06:00;10:00;14:00;18:00;22:00' AS times,
           32 AS eco, 8 AS biz, 2500 AS eco_fare, 4000 AS biz_fare
    UNION ALL SELECT 'Islamabad', 'Lahore', 'Daewoo Express', '06:00;10:00;14:00;18:00;22:00', 32, 8, 2500, 4000
    UNION ALL SELECT 'Lahore', 'Karachi', 'Faisal Movers', '07:00;17:00;21:00', 36, 8, 6500, 9500
    UNION ALL SELECT 'Karachi', 'Lahore', 'Faisal Movers', '07:00;17:00;21:00', 36, 8, 6500, 9500
    UNION ALL SELECT 'Islamabad', 'Murree', 'Niazi Express', '08:00;11:00;14:00;17:00', 28, 4, 800, 1300
    UNION ALL SELECT 'Islamabad', 'Swat', 'Skyways', '07:00;13:00', 32, 8, 2200, 3500
    UNION ALL SELECT 'Lahore', 'Multan', 'Skyways', '08:00;12:00;16:00;20:00', 32, 8, 2200, 3500
) AS r
 WHERE NOT EXISTS (SELECT 1 FROM bus_routes);

INSERT INTO hotels (name, city, address, star_rating, single_rooms, double_rooms, suite_rooms,
                    single_rate, double_rate, suite_rate)
SELECT * FROM (
    SELECT 'Margalla View Hotel' AS n, 'Islamabad' AS c, 'Blue Area, Islamabad' AS a, 4 AS s,
           20 AS sr, 30 AS dr, 5 AS su, 12000 AS r1, 18000 AS r2, 35000 AS r3
    UNION ALL SELECT 'Mall Road Residency', 'Lahore', 'Mall Road, Lahore', 3, 15, 25, 3, 8000, 12500, 25000
    UNION ALL SELECT 'Sea Breeze Hotel', 'Karachi', 'Clifton, Karachi', 4, 20, 30, 6, 11000, 17000, 32000
    UNION ALL SELECT 'Hunza Peaks Inn', 'Hunza', 'Karimabad, Hunza', 3, 10, 15, 2, 9000, 14000, 26000
    UNION ALL SELECT 'Pine Hills Lodge', 'Murree', 'Mall Road, Murree', 3, 12, 18, 2, 7000, 11000, 21000
) AS h
 WHERE NOT EXISTS (SELECT 1 FROM hotels);

INSERT INTO employees (first_name, last_name, email, phone, job_title, department, hire_date, salary)
SELECT * FROM (
    SELECT 'Ali' AS f, 'Raza' AS l, 'ali.raza@travelagency.pk' AS e, '0301-1112223' AS p,
           'Travel Consultant' AS j, 'Sales' AS d, '2024-02-01' AS h, 85000 AS s
    UNION ALL SELECT 'Sara', 'Ahmed', 'sara.ahmed@travelagency.pk', '0302-2223334', 'Tour Guide', 'Operations', '2024-05-15', 70000
    UNION ALL SELECT 'Usman', 'Tariq', 'usman.tariq@travelagency.pk', '0303-3334445', 'Accountant', 'Finance', '2023-11-20', 95000
) AS e
 WHERE NOT EXISTS (SELECT 1 FROM employees);
