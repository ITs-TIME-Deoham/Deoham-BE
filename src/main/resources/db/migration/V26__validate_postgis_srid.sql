-- =============================================================
-- V26: PostGIS spatial reference system validation
-- Ensures SRID 4326 (WGS84) is available for geospatial queries
-- =============================================================

-- Verify that SRID 4326 (WGS84) is available
-- If this fails, PostGIS installation is incomplete in your environment
DO $$
BEGIN
  IF NOT EXISTS(SELECT 1 FROM spatial_ref_sys WHERE srid = 4326) THEN
    RAISE WARNING 'SRID 4326 (WGS84) not found. Ensure PostGIS is fully initialized.';
  ELSE
    RAISE NOTICE 'SRID 4326 (WGS84) is available';
  END IF;
EXCEPTION WHEN OTHERS THEN
  RAISE WARNING 'Could not verify SRID 4326: %', SQLERRM;
END $$;
