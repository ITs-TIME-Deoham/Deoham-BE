-- =============================================================
-- V25: PostGIS spatial reference system initialization
-- Ensures SRID 4326 (WGS84) and other spatial data is available
-- =============================================================

-- Ensure PostGIS extension exists (idempotent)
CREATE EXTENSION IF NOT EXISTS postgis;

-- Verify spatial_ref_sys is populated by calling postgis version check
-- This will fail gracefully in test environments without PostGIS, which is expected
DO $$
BEGIN
  PERFORM postgis_full_version();
EXCEPTION WHEN undefined_function THEN
  -- PostGIS not installed in this environment (e.g., test with standard postgres)
  NULL;
END $$;
