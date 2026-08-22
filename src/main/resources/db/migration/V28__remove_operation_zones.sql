-- Remove FK from dispatch_resources
ALTER TABLE public.dispatch_resources
DROP CONSTRAINT IF EXISTS dispatch_resources_zone_id_fkey,
    DROP CONSTRAINT IF EXISTS dispatch_resources_edge_node_id_fkey;

-- Remove FK from dispatch_requests
ALTER TABLE public.dispatch_requests
DROP CONSTRAINT IF EXISTS dispatch_requests_zone_id_fkey,
    DROP CONSTRAINT IF EXISTS dispatch_requests_edge_node_id_fkey;

-- Remove zone indexes from referencing tables
DROP INDEX IF EXISTS public.idx_dispatch_resources_zone;
DROP INDEX IF EXISTS public.idx_dispatch_requests_zone;

-- Drop zone columns
ALTER TABLE public.dispatch_resources
DROP COLUMN IF EXISTS zone_id;

ALTER TABLE public.dispatch_requests
DROP COLUMN IF EXISTS zone_id;

-- Remove indexes owned by the old zone model
DROP INDEX IF EXISTS public.idx_operation_zones_coverage;
DROP INDEX IF EXISTS public.idx_edge_nodes_coverage;

-- Finally remove old table
DROP TABLE IF EXISTS public.operation_zones;
DROP TABLE IF EXISTS public.edge_nodes;