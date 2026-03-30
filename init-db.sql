-- This script runs automatically when the MySQL container starts for the first time.
-- It creates the three databases used by the microservices.

CREATE DATABASE IF NOT EXISTS auth_Service;
CREATE DATABASE IF NOT EXISTS account_Service;
CREATE DATABASE IF NOT EXISTS transaction_Service;
