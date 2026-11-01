-- Runs once, on first start of an EMPTY pgdata volume (Section 7.1).
-- Passwords are read from container environment variables via psql's \getenv (psql 15+, we use 16).
\getenv user_pw    USER_DB_PASSWORD
\getenv catalog_pw CATALOG_DB_PASSWORD
\getenv order_pw   ORDER_DB_PASSWORD

CREATE ROLE user_svc    LOGIN PASSWORD :'user_pw';
CREATE ROLE catalog_svc LOGIN PASSWORD :'catalog_pw';
CREATE ROLE order_svc   LOGIN PASSWORD :'order_pw';

CREATE DATABASE user_db    OWNER user_svc;
CREATE DATABASE catalog_db OWNER catalog_svc;
CREATE DATABASE order_db   OWNER order_svc;

-- Only the owning role may connect (superuser still can, for admin/backup).
REVOKE CONNECT ON DATABASE user_db    FROM PUBLIC;
REVOKE CONNECT ON DATABASE catalog_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE order_db   FROM PUBLIC;

GRANT CONNECT ON DATABASE user_db    TO user_svc;
GRANT CONNECT ON DATABASE catalog_db TO catalog_svc;
GRANT CONNECT ON DATABASE order_db   TO order_svc;