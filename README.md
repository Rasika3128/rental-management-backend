# Rental Management System — Backend

A Spring Boot REST API powering a full-stack rental management platform, where tenants can submit rent payments and administrators can review and approve them.

## Features

- **JWT-based Authentication** — Secure register/login with role-based access (USER / ADMIN)
- **Password Security** — BCrypt password hashing
- **File Uploads** — ID proof, passport photo, and payment screenshot handling via multipart requests
- **Tenant APIs** — View profile, rent payment history, submit new rent payments
- **Admin APIs** — View all tenants, review pending payments, accept/reject with remarks
- **Role-based Route Protection** — Spring Security enforces access rules at the API level

## Tech Stack

- **Java 17** + **Spring Boot 4**
- **Spring Security** + **JWT (jjwt)**
- **Spring Data JPA** + **Hibernate**
- **MySQL / MariaDB**
- **Maven**

## API Overview

| Method | Endpoint | Description | Access |
|--------|----------|--------------|--------|
| POST | `/api/auth/register` | Register a new tenant | Public |
| POST | `/api/auth/login` | Login and receive JWT | Public |
| GET | `/api/user/profile` | Get logged-in user's profile | USER |
| GET | `/api/user/rent-history` | Get rent payment history | USER |
| POST | `/api/user/rent-payment` | Submit a rent payment with screenshot | USER |
| GET | `/api/admin/users` | List all users with payment summary | ADMIN |
| GET | `/api/admin/users/{id}` | Get a specific user's details + history | ADMIN |
| GET | `/api/admin/payments/pending` | List all pending payments | ADMIN |
| PUT | `/api/admin/payments/{id}/accept` | Approve a payment | ADMIN |
| PUT | `/api/admin/payments/{id}/reject` | Reject a payment with a remark | ADMIN |

## Running Locally

1. Clone the repo
2. Create a MySQL database named `rental_db`
3. Update `src/main/resources/application.properties` with your database credentials
4. Run:
```bash
   mvn spring-boot:run
```
5. API will be available at `http://localhost:8080`

## Related Repository

Frontend (React): [rental-management-frontend](https://github.com/Rasika3128/rental-management-frontend)