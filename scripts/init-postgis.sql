-- Create PostGIS extension and initialize spatial reference systems
CREATE EXTENSION IF NOT EXISTS postgis;

-- Populate spatial_ref_sys if not already done
SELECT postgis_full_version();

-- Create additional useful extensions for spatial databases
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Create uuid extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant privileges on PostGIS objects to the deoham user
GRANT ALL PRIVILEGES ON SCHEMA public TO deoham;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO deoham;
