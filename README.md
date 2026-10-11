# cognevance_Cartly-an-Enterprise-E-Commerce-Analytics-Platform.


# Cartly: Enterprise E-Commerce & Analytics Platform

A scalable full-stack e-commerce platform with role-based access, payments, email notifications, cloud image storage and an admin analytics dashboard.

## Features (mapped to project tasks)
| # | Task | Implementation |
|---|---|---|
| 1 | Secure auth and RBAC | JWT, BCrypt, roles CUSTOMER and ADMIN, method-level `@PreAuthorize` |
| 2 | Product, order, customer modules | Product CRUD with soft delete, order lifecycle, customer list with spend |
| 3 | Payments and email | Razorpay (HMAC-verified) or demo mode; SMTP order and welcome emails |
| 4 | Admin dashboard and reports | Revenue, orders by status, top products, category sales, low stock, CSV export |
| 5 | Cloud storage | Cloudinary upload (or local disk), image type and size validation |
| 6 | Performance | Pagination, DB indexes, caching, gzip, batch fetching, optimistic locking, lazy-loaded routes and images |
| 7 | Deployment | Dockerfile, docker-compose, Render and Vercel steps |
| 8 | Documentation | This README, API table, workflows, schema |

## Architecture
```
React (Vite)  --HTTPS/JSON + JWT-->  Spring Boot API  --JPA-->  MySQL
   |                                   |-- Razorpay (payments)
   |                                   |-- Cloudinary (images)
   '-- Razorpay checkout               '-- SMTP (emails)
```
Backend layers: controller, service, repository, model, dto, exception, security, config.

## Run locally
Backend (Java 17, Maven):
```bash
cd backend && mvn spring-boot:run
```
Frontend (Node 18+):
```bash
cd frontend && cp .env.example .env && npm install && npm run dev
```
Open http://localhost:5173. Demo admin: `admin@cartly.com` / `Admin@123`. Sample products are seeded.
With Docker: `docker compose up --build` (MySQL and backend).

## Environment variables (backend)
| Variable | Purpose | Default |
|---|---|---|
| DB_URL, DB_USER, DB_PASSWORD | MySQL connection | in-memory H2 |
| JWT_SECRET | JWT signing key (32+ chars) | dev key |
| CORS_ORIGINS | Allowed frontend URLs, comma separated | http://localhost:5173 |
| BASE_URL | Public backend URL (for local image links) | http://localhost:8080 |
| PAYMENT_PROVIDER | `mock` or `razorpay` | mock |
| RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET | Razorpay keys (test or live) | empty |
| STORAGE_PROVIDER | `local` or `cloudinary` | local |
| CLOUDINARY_CLOUD_NAME, CLOUDINARY_UPLOAD_PRESET | Cloudinary unsigned preset | empty |
| SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD | SMTP; emails are only logged when unset | unset |

Frontend: `VITE_API_URL` (backend URL).

## REST API
Base `/api`. Protected routes need `Authorization: Bearer <token>`.

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | /auth/register, /auth/login | Public | Sign up and log in |
| GET | /products?search&category&sort&page&size | Public | Paginated catalogue |
| GET | /products/categories | Public | Cached category list |
| GET | /products/{id} | Public | Product details |
| POST, PUT, DELETE | /products, /products/{id} | Admin | Manage products |
| POST | /products/upload | Admin | Upload image, returns `{url}` |
| POST | /orders | Customer | Create order from cart |
| GET | /orders/my | Customer | My orders |
| GET | /orders/{id} | Owner or Admin | Order details |
| PUT | /orders/{id}/cancel | Customer | Cancel unpaid order |
| GET | /orders?status&page&size | Admin | All orders |
| PUT | /orders/{id}/status | Admin | Move to SHIPPED, DELIVERED or CANCELLED |
| POST | /payments/{orderId}/init | Customer | Create gateway order |
| POST | /payments/{orderId}/verify | Customer | Verify payment, mark PAID |
| GET | /admin/analytics | Admin | Dashboard metrics |
| GET | /admin/customers | Admin | Customers with spend |
| GET | /admin/reports/orders.csv | Admin | CSV report |

## Workflows
**Checkout:** cart (browser) -> `POST /orders` (stock reserved, status PENDING) -> `POST /payments/{id}/init` -> Razorpay checkout -> `POST /payments/{id}/verify` (HMAC check) -> status PAID -> confirmation email.

**Order lifecycle:** PENDING -> PAID -> SHIPPED -> DELIVERED. PENDING orders can be cancelled by the customer; admins can cancel before delivery. Cancelling restores stock.

## Database schema
`users`, `products` (indexed by category and name), `orders` (indexed by user, status, date), `order_items` (price snapshot per item). See `database/schema.sql`.

## Performance and security notes
- Pagination on products and orders; indexes on frequent filters
- `@Cacheable` categories, gzip responses, Hikari pool, Hibernate batch fetching
- Optimistic locking (`@Version`) on products prevents overselling
- Price taken from the database at order time, never from the client
- Razorpay signature verified server-side; mock mode is for demos only
- Frontend: route-level code splitting, lazy images, debounced search

## Deployment
1. **MySQL:** Railway or Aiven. Copy the JDBC URL.
2. **Backend (Render, Docker):** root directory `backend`, set the environment variables above. Use `STORAGE_PROVIDER=cloudinary` because Render's disk is temporary. Health check path `/actuator/health`.
3. **Frontend (Vercel):** root `frontend`, build `npm run build`, output `dist`, env `VITE_API_URL`.
4. Set `CORS_ORIGINS` on the backend to the Vercel URL.

## Screenshots
_Add screenshots: shop, cart, orders, admin dashboard, admin products._

## Links
- Live frontend: _add link_
- Live backend: _add link_
- GitHub: _add link_