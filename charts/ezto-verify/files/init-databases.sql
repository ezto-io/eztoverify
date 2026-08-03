-- Initialize databases for the EZTO Verify platform (local development).
-- Runs automatically the first time the PostgreSQL container starts.

CREATE DATABASE "eztoverify";   -- idp (Keycloak fork) datastore
CREATE DATABASE "jobrnr";       -- background (jobrunr) service
CREATE DATABASE "urlshortner";  -- url-shortener service
CREATE DATABASE "socket";       -- socket service
