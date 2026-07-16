-- Create PostGIS extension and initialize spatial reference systems
CREATE EXTENSION IF NOT EXISTS postgis;

-- Upgrade PostGIS extensions to ensure spatial_ref_sys is fully populated
SELECT postgis_extensions_upgrade();


-- Create additional useful extensions for spatial databases
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Create uuid extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant privileges on PostGIS objects to the deoham user
GRANT ALL PRIVILEGES ON SCHEMA public TO deoham;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO deoham;
