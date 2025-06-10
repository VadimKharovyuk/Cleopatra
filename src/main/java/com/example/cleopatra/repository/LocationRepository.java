package com.example.cleopatra.repository;

import com.example.cleopatra.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query("SELECT l FROM Location l WHERE " +
            "ABS(l.latitude - :lat) < :precision AND " +
            "ABS(l.longitude - :lng) < :precision")
    Optional<Location> findByCoordinatesWithPrecision(
            @Param("lat") Double latitude,
            @Param("lng") Double longitude,
            @Param("precision") Double precision);

    List<Location> findByPlaceNameContainingIgnoreCase(String placeName);
    List<Location> findByCity(String city);
    List<Location> findByCountry(String country);
    Optional<Location> findByMapboxPlaceId(String mapboxPlaceId);
}