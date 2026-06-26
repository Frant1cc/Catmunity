package org.catmunity.service;

import org.catmunity.model.vo.*;

public interface DashboardService {

    DashboardOverviewVO getOverview();

    UserGrowthTrendVO getUserGrowthTrend(String dimension, Integer days);

    ContentStatsVO getContentStats();

    AIFeatureUsageVO getAIFeatureUsage();

    ModerationStatsVO getModerationStats();

    CatBreedDistributionVO getCatBreedDistribution();
}