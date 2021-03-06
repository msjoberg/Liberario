/*    Liberario
 *    Copyright (C) 2013 Torsten Grote
 *
 *    This program is Free Software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as
 *    published by the Free Software Foundation, either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.grobox.liberario;

import java.util.ArrayList;
import java.util.List;

import de.cketti.library.changelog.ChangeLog;
import de.schildbach.pte.NetworkProvider;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity {
	private MainPagerAdapter mainPagerAdapter;
	private ViewPager mViewPager;

	static final int CHANGED_NETWORK_PROVIDER = 1;
	static final int CHANGED_HOME = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mViewPager = (ViewPager) findViewById(R.id.pager);

		// don't recreate the fragments when changing tabs
		mViewPager.setOffscreenPageLimit(3);

		final ActionBar actionBar = getActionBar();

		// Specify that tabs should be displayed in the action bar.
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create a tab listener that is called when the user changes tabs.
		ActionBar.TabListener tabListener = new ActionBar.TabListener() {
			@Override
			public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
				// show the given tab
				mViewPager.setCurrentItem(tab.getPosition());
			}
			@Override
			public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
				// hide the given tab
			}
			@Override
			public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
				// probably ignore this event
			}
		};

		List<String> fragments = new ArrayList<>();

		fragments.add(DirectionsFragment.class.getName());
		actionBar.addTab(actionBar.newTab().setIcon(android.R.drawable.ic_menu_directions).setTabListener(tabListener));

		actionBar.addTab(actionBar.newTab().setIcon(R.drawable.ic_action_star).setTabListener(tabListener));
		fragments.add(FavTripsFragment.class.getName());

		actionBar.addTab(actionBar.newTab().setIcon(R.drawable.ic_tab_stations).setTabListener(tabListener));
		fragments.add(StationsFragment.class.getName());

		mainPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), this, fragments);

		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				// When swiping between pages, select the corresponding tab.
				getActionBar().setSelectedNavigationItem(position);
			}
		});

		mViewPager.setAdapter(mainPagerAdapter);

		// show about screen and make sure a transport network is selected
		checkFirstRun();

		// show Changelog
		HoloChangeLog cl = new HoloChangeLog(this);
		if(cl.isFirstRun() && !cl.isFirstRunEver()) {
			cl.getLogDialog().show();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_settings:
				startActivityForResult(new Intent(this, PickNetworkProviderActivity.class), CHANGED_NETWORK_PROVIDER);

				return true;
			case R.id.action_changelog:
				new HoloChangeLog(this).getFullLogDialog().show();

				return true;
			case R.id.action_about:
				startActivity(new Intent(this, AboutActivity.class));

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if(requestCode == CHANGED_NETWORK_PROVIDER && resultCode == RESULT_OK) {
			NetworkProvider np = NetworkProviderFactory.provider(Preferences.getNetworkId(this));
			onNetworkProviderChanged(np);
		}
	}

	public void onNetworkProviderChanged(NetworkProvider np) {
		// get and set new network name for action bar
		SharedPreferences settings = getSharedPreferences(Preferences.PREFS, Context.MODE_PRIVATE);
		getActionBar().setSubtitle(settings.getString("NetworkId", "???"));

		if(getSupportFragmentManager().getFragments() != null) {
			// call this method for each fragment
			for(final Fragment fragment : getSupportFragmentManager().getFragments()) {
				if(fragment instanceof LiberarioFragment) {
					((LiberarioFragment) fragment).onNetworkProviderChanged(np);
				} else if(fragment instanceof LiberarioListFragment) {
					((LiberarioListFragment) fragment).onNetworkProviderChanged(np);
				}
			}
		}
	}

	private void checkFirstRun() {
		SharedPreferences settings = getSharedPreferences(Preferences.PREFS, Context.MODE_PRIVATE);
		boolean firstRun = settings.getBoolean("FirstRun", true);

		// show about page at first run
		if(firstRun) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("FirstRun", false);
			editor.apply();

			startActivity(new Intent(this, AboutActivity.class));
		}

		String network = settings.getString("NetworkId", null);

		// return if no network is set
		if(network == null) {
			startActivityForResult(new Intent(this, PickNetworkProviderActivity.class), CHANGED_NETWORK_PROVIDER);
		}
		else {
			getActionBar().setSubtitle(network);
		}

	}


	public static class HoloChangeLog extends ChangeLog {
		public static final String DARK_THEME_CSS =
				"body { color: #f3f3f3; font-size: 0.9em; background-color: #282828; } h1 { font-size: 1.3em; } ul { padding-left: 2em; }";

		public HoloChangeLog(Context context) {
			super(new ContextThemeWrapper(context, R.style.AppTheme), DARK_THEME_CSS);
		}
	}

}
