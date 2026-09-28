package com.randevupazaryeri.config;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DevDataSeederTest {

    @Test
    void nearbySeedVenuesHaveVariedCatalogsAndPhotographedMultiServiceTeams() {
        var venues = DevDataSeeder.NEARBY_VENUES;

        assertThat(venues).hasSize(9);

        Set<Integer> teamSizes = new HashSet<>();
        for (var venue : venues) {
            assertThat(venue.services()).hasSizeBetween(3, 5);
            assertThat(venue.employees()).hasSizeBetween(2, 4);
            teamSizes.add(venue.employees().size());

            Set<String> serviceNames = venue.services().stream()
                    .map(DevDataSeeder.SeedService::name)
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(venue.employees()).allSatisfy(employee -> {
                assertThat(employee.photoUrl()).startsWith("https://images.pexels.com/");
                assertThat(employee.serviceNames()).isNotEmpty();
                assertThat(serviceNames).containsAll(employee.serviceNames());
            });
            assertThat(venue.employees().stream().anyMatch(employee -> employee.serviceNames().size() > 1)).isTrue();
        }

        assertThat(teamSizes).contains(2, 3, 4);
    }

    @Test
    void employeeNameMatchingTreatsTurkishDotlessIAsAsciiI() {
        assertThat(DevDataSeeder.normalizeEmployeeName("Selin Aydın"))
                .isEqualTo(DevDataSeeder.normalizeEmployeeName("Selin Aydin"));
    }
}