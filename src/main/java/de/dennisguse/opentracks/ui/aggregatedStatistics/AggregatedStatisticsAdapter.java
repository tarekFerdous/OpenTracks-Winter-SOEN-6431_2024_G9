package de.dennisguse.opentracks.ui.aggregatedStatistics;

import android.content.Context;
import android.content.Intent;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.CalendarActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.data.models.SpeedFormatter;
import de.dennisguse.opentracks.databinding.AggregatedStatsListItemBinding;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.stats.EnhancedTrackStatistics;
import de.dennisguse.opentracks.ui.aggregatedStatistics.SeasonStats.SeasonStatActivity;
import de.dennisguse.opentracks.ui.aggregatedStatistics.daySpecificStats.DaySpecificActivity;
import de.dennisguse.opentracks.util.StringUtils;

public class AggregatedStatisticsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private AggregatedStatistics aggregatedStatistics;
    private EnhancedTrackStatistics enhancedTrackStatistics;
    private final Context context;

    public AggregatedStatisticsAdapter(Context context, AggregatedStatistics aggregatedStatistics) {
        this.context = context;
        this.aggregatedStatistics = aggregatedStatistics;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AggregatedStatsListItemBinding.inflate(LayoutInflater.from(parent.getContext())));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder) holder;

        AggregatedStatistics.AggregatedStatistic aggregatedStatistic = aggregatedStatistics.getItem(position);

        String type = aggregatedStatistic.getActivityTypeLocalized();
        if (ActivityType.findByLocalizedString(context, type).isShowSpeedPreferred()) {
            viewHolder.setSpeed(aggregatedStatistic);
        } else {
            viewHolder.setPace(aggregatedStatistic);
        }
    }

    @Override
    public int getItemCount() {
        if (aggregatedStatistics == null) {
            return 0;
        }
        return aggregatedStatistics.getCount();
    }

    public void swapData(AggregatedStatistics aggregatedStatistics) {
        this.aggregatedStatistics = aggregatedStatistics;
        this.notifyDataSetChanged();
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        for (int i = 0; i < aggregatedStatistics.getCount(); i++) {
            categories.add(aggregatedStatistics.getItem(i).getActivityTypeLocalized());
        }
        return categories;
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private final AggregatedStatsListItemBinding viewBinding;
        private UnitSystem unitSystem = UnitSystem.defaultUnitSystem();
        private boolean reportSpeed;

        public ViewHolder(AggregatedStatsListItemBinding viewBinding) {
            super(viewBinding.getRoot());
            this.viewBinding = viewBinding;

            viewBinding.listOfSeasonsBtn234.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = viewBinding.getRoot().getContext();
                    Intent intent = new Intent(context, SeasonStatActivity.class);
                    context.startActivity(intent);
                }
            });

            viewBinding.runsAndLiftsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), CalendarActivity.class);
                    intent.putExtra("Display Fields", "Runs and Lifts");
                    v.getContext().startActivity(intent);
                }
            });

            viewBinding.dayStatisticsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Context context = viewBinding.getRoot().getContext();
                    Intent intent = new Intent(context, DayStatisticsActivity.class);
                    context.startActivity(intent);
                }
            });

            viewBinding.elevationAndSpeedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), CalendarActivity.class);
                    intent.putExtra("Display Fields", "Elevation and Speed");
                    v.getContext().startActivity(intent);
                }
            });
        }

        public void setSpeed(AggregatedStatistics.AggregatedStatistic aggregatedStatistic) {
            setCommonValues(aggregatedStatistic);

            SpeedFormatter formatter = SpeedFormatter.Builder().setUnit(unitSystem).setReportSpeedOrPace(reportSpeed).build(context);
            {
                Pair<String, String> parts = formatter.getSpeedParts(aggregatedStatistic.getTrackStatistics().getAverageMovingSpeed());
                viewBinding.aggregatedStatsAvgRate.setText(parts.first);
                viewBinding.aggregatedStatsAvgRateUnit.setText(parts.second);
                viewBinding.aggregatedStatsAvgRateLabel.setText(context.getString(R.string.stats_average_moving_speed));
            }

            {
                Pair<String, String> parts = formatter.getSpeedParts(aggregatedStatistic.getTrackStatistics().getMaxSpeed());
                viewBinding.aggregatedStatsMaxRate.setText(parts.first);
                viewBinding.aggregatedStatsMaxRateUnit.setText(parts.second);
                viewBinding.aggregatedStatsMaxRateLabel.setText(context.getString(R.string.stats_max_speed));
            }
        }

        public void setPace(AggregatedStatistics.AggregatedStatistic aggregatedStatistic) {
            setCommonValues(aggregatedStatistic);

            SpeedFormatter formatter = SpeedFormatter.Builder().setUnit(unitSystem).setReportSpeedOrPace(reportSpeed).build(context);
            {
                Pair<String, String> parts = formatter.getSpeedParts(aggregatedStatistic.getTrackStatistics().getAverageMovingSpeed());
                viewBinding.aggregatedStatsAvgRate.setText(parts.first);
                viewBinding.aggregatedStatsAvgRateUnit.setText(parts.second);
                viewBinding.aggregatedStatsAvgRateLabel.setText(context.getString(R.string.stats_average_moving_pace));
            }

            {
                Pair<String, String> parts = formatter.getSpeedParts(aggregatedStatistic.getTrackStatistics().getMaxSpeed());
                viewBinding.aggregatedStatsMaxRate.setText(parts.first);
                viewBinding.aggregatedStatsMaxRateUnit.setText(parts.second);
                viewBinding.aggregatedStatsMaxRateLabel.setText(R.string.stats_fastest_pace);
            }
        }

        //TODO Check preference handling.
        private void setCommonValues(AggregatedStatistics.AggregatedStatistic aggregatedStatistic) {
            String activityType = aggregatedStatistic.getActivityTypeLocalized();

            reportSpeed = PreferencesUtils.isReportSpeed(activityType);
            unitSystem = PreferencesUtils.getUnitSystem();

            viewBinding.activityIcon.setImageResource(getIcon(aggregatedStatistic));
            viewBinding.aggregatedStatsTypeLabel.setText(activityType);
            viewBinding.aggregatedStatsNumTracks.setText(StringUtils.valueInParentheses(String.valueOf(aggregatedStatistic.getCountTracks())));

            Pair<String, String> parts = DistanceFormatter.Builder()
                    .setUnit(unitSystem)
                    .build(context).getDistanceParts(aggregatedStatistic.getTrackStatistics().getTotalDistance());
            viewBinding.aggregatedStatsDistance.setText(parts.first);
            viewBinding.aggregatedStatsDistanceUnit.setText(parts.second);

            viewBinding.aggregatedStatsTime.setText(StringUtils.formatElapsedTime(aggregatedStatistic.getTrackStatistics().getMovingTime()));

            if (activityType.equals("skiing")){
                viewBinding.aggregatedStatsSlopePercentLabel.setVisibility(View.VISIBLE);
                viewBinding.aggregatedStatsSlopePercent.setVisibility(View.VISIBLE);
                viewBinding.aggregatedStatsSlopePercentUnit.setVisibility(View.VISIBLE);

                if (aggregatedStatistic.getTrackStatistics().getSlopePercent() == null) {
                    viewBinding.aggregatedStatsSlopePercent.setText("0");
                } else {
                    viewBinding.aggregatedStatsSlopePercent.setText(String.valueOf(aggregatedStatistic.getTrackStatistics().getSlopePercent()));
                }
                viewBinding.aggregatedStatsSlopePercentLabel.setText("Average Slope %");
                viewBinding.aggregatedStatsSlopePercentUnit.setText("%");
            }

//            for the time duration of skiing
            if (activityType.equals("skiing")){

                viewBinding.totalSkiingDurationTextView.setVisibility(View.VISIBLE);
                viewBinding.aggregatedStatsSkiingDurationLabel.setVisibility(View.VISIBLE);
                viewBinding.aggregatedStatsSkiingDurationLabel.setText("TOTAL TIME SKIING");
                if (aggregatedStatistic.durationTIme() == null) {
                    viewBinding.totalSkiingDurationTextView.setText("00:00:00");
                } else {
//                    long temp = aggregatedStatistic.getTrackStatistics().getTotalTime().getSeconds() - aggregatedStatistic.getTrackStatistics().getMovingTime().getSeconds();
//                    System.out.println(enhancedTrackStatistics.getTimeOnChairlift());
                    viewBinding.totalSkiingDurationTextView.setText(String.valueOf(aggregatedStatistic.getTrackStatistics().getSkiingDuration().getSeconds()));
                }
            }

            if (activityType.equals("skiing")){
                viewBinding.aggregatedStatsChairliftLabel.setVisibility(View.VISIBLE);
                viewBinding.aggregatedStatsChairliftTime.setVisibility(View.VISIBLE);

                if (aggregatedStatistic.getTrackStatistics().getTotalChairliftTime() == Duration.ZERO) {
                    viewBinding.aggregatedStatsChairliftTime.setText("0");
                } else {
                    viewBinding.aggregatedStatsChairliftTime.setText(String.valueOf(aggregatedStatistic.getTrackStatistics().getTotalChairliftTime()));
                }
                viewBinding.aggregatedStatsChairliftLabel.setText("TOTAL CHAIRLIFT TIME");
            }
        }

        private int getIcon(AggregatedStatistics.AggregatedStatistic aggregatedStatistic) {
            String localizedActivityType = aggregatedStatistic.getActivityTypeLocalized();
            return ActivityType.findByLocalizedString(context, localizedActivityType)
                    .getIconDrawableId();
        }
    }
}
