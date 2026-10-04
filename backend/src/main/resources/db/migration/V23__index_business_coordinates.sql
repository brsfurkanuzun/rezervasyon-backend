CREATE INDEX idx_businesses_lat_lng ON businesses (latitude, longitude) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
