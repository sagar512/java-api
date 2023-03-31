package com.peopleapp.repository;

import com.peopleapp.model.RecentActiveNetwork;

import java.util.List;

public interface CustomRecentActiveNetworkRepository {

    /* Returns all the recently modified networks for provided category sorted desc by weightage */
    List<RecentActiveNetwork> getRecentNetworksByCategory(String userId, String networkCategory, int newMemberWeightage,
                                                          int newNetworkWeightage);

    /* Returns all the recently modified networks sorted desc by weightage */
    List<RecentActiveNetwork> getTopRecentNetworks(String userId, int newMemberWeightage, int newNetworkWeightage);
}
