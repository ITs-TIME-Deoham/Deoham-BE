-- =============================================================
-- V27: Fix PostGIS SRID initialization
-- Properly upgrades PostGIS extensions and verifies SRID 4326
-- =============================================================

-- Upgrade PostGIS extensions to ensure spatial_ref_sys is fully populated
-- This fixes the issue where V25/V26 only called postgis_full_version()
DO $$
BEGIN
  PERFORM postgis_extensions_upgrade();
EXCEPTION WHEN undefined_function THEN
  -- PostGIS not installed in this environment (e.g., test with standard postgres)
  NULL;
END $$;

-- Verify that SRID 4326 (WGS84) is available
-- If this fails, spatial_ref_sys was not properly initialized
DO $$
BEGIN
  IF NOT EXISTS(SELECT 1 FROM spatial_ref_sys WHERE srid = 4326) THEN
    RAISE EXCEPTION 'SRID 4326 (WGS84) not found in spatial_ref_sys. '
      'PostGIS installation is incomplete. '
      'For Docker environments: run "docker compose down -v && docker compose up" to reinitialize. '
      'For local Postgres: ensure PostGIS extension is properly installed.';
  END IF;
END $$;
