MaxMind GeoLite2 City database — bring your own
================================================

Keycloak resolves the geo-location of login requests from a GeoLite2 City
database. At runtime it is read from MinIO object storage, NOT from this
directory:

    GeoReader.java -> ${MINIO_PUBLIC_URL}/maxmind/GeoLite2-City.mmdb

The database file (GeoLite2-City.mmdb, ~70 MB) is NOT committed to this repo.
MaxMind's GeoLite2 End User License Agreement does not permit redistributing
the database, so each operator must obtain their own copy. Geo-lookups simply
return UNKNOWN until one is provided (non-fatal).

How to provide it
-----------------
1. Create a free MaxMind account and generate a license key:
   https://www.maxmind.com/en/geolite2/signup
2. Download "GeoLite2 City" in MaxMind DB (.mmdb) format and place it here:
       idp/keycloak/public_files/maxmind/GeoLite2-City.mmdb
3. Seed it to the MinIO public bucket so Keycloak can read it:
       mc alias set local http://localhost:9000 minioadmin minioadmin123
       mc cp idp/keycloak/public_files/maxmind/GeoLite2-City.mmdb \
             local/ezto-verify/public/maxmind/GeoLite2-City.mmdb

The .mmdb file is git-ignored, so it will not be committed.

Note: latitude and longitude are not precise and should not be used to
identify a particular street address or household.
