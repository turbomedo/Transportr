package de.grobox.liberario.trips.search;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import java.util.Calendar;

import javax.inject.Inject;

import de.grobox.liberario.R;
import de.grobox.liberario.activities.TransportrActivity;
import de.grobox.liberario.fragments.TimeDateFragment;
import de.grobox.liberario.fragments.TransportrFragment;
import de.grobox.liberario.locations.LocationGpsView;
import de.grobox.liberario.locations.LocationView;
import de.grobox.liberario.locations.WrapLocation;
import de.grobox.liberario.networks.TransportNetwork;
import de.grobox.liberario.utils.DateUtils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.animation.Animation.RELATIVE_TO_SELF;
import static de.grobox.liberario.data.locations.FavoriteLocation.FavLocationType.FROM;
import static de.grobox.liberario.data.locations.FavoriteLocation.FavLocationType.TO;
import static de.grobox.liberario.utils.DateUtils.getDate;
import static de.grobox.liberario.utils.DateUtils.getTime;
import static de.grobox.liberario.utils.DateUtils.isNow;

public class DirectionsFragment extends TransportrFragment {

	@Inject ViewModelProvider.Factory viewModelFactory;

	private View timeIcon;
	private TextView date, time;
	private LocationGpsView from;
	private LocationView to;
	private CardView fromCard, toCard;

	private DirectionsViewModel viewModel;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_directions_form, container, false);
		getComponent().inject(this);

		setHasOptionsMenu(true);
		Toolbar toolbar = v.findViewById(R.id.toolbar);
		timeIcon = toolbar.findViewById(R.id.timeIcon);
		date = toolbar.findViewById(R.id.date);
		time = toolbar.findViewById(R.id.time);

		fromCard = v.findViewById(R.id.fromCard);
		from = v.findViewById(R.id.fromLocation);
		toCard = v.findViewById(R.id.toCard);
		to = v.findViewById(R.id.toLocation);

		((TransportrActivity) getActivity()).setSupportActionBar(toolbar);
		ActionBar ab = ((TransportrActivity) getActivity()).getSupportActionBar();
		if (ab != null) {
			ab.setDisplayShowHomeEnabled(true);
			ab.setDisplayHomeAsUpEnabled(true);
			ab.setDisplayShowCustomEnabled(true);
			ab.setDisplayShowTitleEnabled(false);
		}

		viewModel = ViewModelProviders.of(getActivity(), viewModelFactory).get(DirectionsViewModel.class);
		TransportNetwork network = viewModel.getTransportNetwork().getValue();
		if (network == null) throw new IllegalStateException();
		from.setTransportNetwork(network);
		to.setTransportNetwork(network);

		from.setType(FROM);
		to.setType(TO);

		viewModel.getHome().observe(this, homeLocation -> {
			from.setHomeLocation(homeLocation);
			to.setHomeLocation(homeLocation);
		});
		viewModel.getWork().observe(this, workLocation -> {
			from.setWorkLocation(workLocation);
			to.setWorkLocation(workLocation);
		});
		viewModel.getLocations().observe(this, favoriteLocations -> {
			if (favoriteLocations == null) return;
			from.setFavoriteLocations(favoriteLocations);
			to.setFavoriteLocations(favoriteLocations);
		});
		viewModel.getFromLocation().observe(this, this::onFromLocationUpdated);
		viewModel.getViaLocation().observe(this, this::onViaLocationUpdated);
		viewModel.getToLocation().observe(this, this::onToLocationUpdated);
		viewModel.getCalendar().observe(this, this::onCalendarUpdated);

		setupClickListeners();

		return v;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.directions, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_swap_locations) {
			swapLocations();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setupClickListeners() {
		OnClickListener onTimeClickListener = view -> {
			TimeDateFragment fragment = TimeDateFragment.newInstance(viewModel.getCalendar().getValue());
			fragment.setTimeDateListener(viewModel);
			fragment.show(getActivity().getSupportFragmentManager(), TimeDateFragment.TAG);
		};
		OnLongClickListener onTimeLongClickListener = view -> {
			viewModel.resetCalender();
			return true;
		};

		timeIcon.setOnClickListener(onTimeClickListener);
		date.setOnClickListener(onTimeClickListener);
		time.setOnClickListener(onTimeClickListener);

		timeIcon.setOnLongClickListener(onTimeLongClickListener);
		date.setOnLongClickListener(onTimeLongClickListener);
		time.setOnLongClickListener(onTimeLongClickListener);

		from.setLocationViewListener(viewModel);
		to.setLocationViewListener(viewModel);
	}

	void onCalendarUpdated(@Nullable Calendar calendar) {
		if (calendar == null) return;
		if (isNow(calendar)) {
			time.setText(R.string.now);
			date.setVisibility(GONE);
		} else if (DateUtils.isToday(calendar)) {
			time.setText(getTime(getContext(), calendar.getTime()));
			date.setVisibility(GONE);
		} else {
			time.setText(getTime(getContext(), calendar.getTime()));
			date.setText(getDate(getContext(), calendar.getTime()));
			date.setVisibility(VISIBLE);
		}
	}

	void onFromLocationUpdated(@Nullable WrapLocation location) {
		from.setLocation(location);
	}

	void onViaLocationUpdated(@Nullable WrapLocation location) {
//		via.setLocation(location);
	}

	void onToLocationUpdated(@Nullable WrapLocation location) {
		to.setLocation(location);
	}

	private void swapLocations() {
		Animation slideUp = new TranslateAnimation(RELATIVE_TO_SELF, 0.0f, RELATIVE_TO_SELF, 0.0f, RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, fromCard.getY() * -1);
		slideUp.setDuration(400);
		slideUp.setFillAfter(true);
		slideUp.setFillEnabled(true);

		Animation slideDown = new TranslateAnimation(RELATIVE_TO_SELF, 0.0f, RELATIVE_TO_SELF, 0.0f, RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, fromCard.getY());
		slideDown.setDuration(400);
		slideDown.setFillAfter(true);
		slideDown.setFillEnabled(true);

		fromCard.startAnimation(slideDown);
		toCard.startAnimation(slideUp);

		slideUp.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			@Override
			public void onAnimationEnd(Animation animation) {
				// swap location objects
				WrapLocation tmp = to.getLocation();
				if(!from.isSearching()) {
					viewModel.setToLocation(from.getLocation());
				} else {
					// TODO: GPS currently only supports from location, so don't swap it for now
					viewModel.setToLocation(null);
				}
				viewModel.setFromLocation(tmp);

				fromCard.clearAnimation();
				toCard.clearAnimation();

				viewModel.search();
			}
		});
	}

}