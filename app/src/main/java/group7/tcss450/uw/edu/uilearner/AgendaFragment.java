package group7.tcss450.uw.edu.uilearner;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;

import group7.tcss450.uw.edu.uilearner.auth.ChooseRoleFragment;
import group7.tcss450.uw.edu.uilearner.auth.SignInFragment;
import group7.tcss450.uw.edu.uilearner.util.DateUtil;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 *
 * Calls and displays the Daily Agenda cards for the User.
 * @author Connor, Myles
 */
public class AgendaFragment extends Fragment implements AgendaAdapter.OnEditButtonInteractionListener {

    private int mColumnCount = 1;
    private static final String TAG = "AGENDA";

    protected OnListFragmentInteractionListener mListener;
    private RecyclerView mRecyclerView;
    private String mUid;
    private String mRole;
    private ArrayList<String> mValues;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AgendaFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.agenda_list);
        return view;
    }


    /**
     * Gets the Bundle arguments passed from AgendaActivity that holds the uuid
     * needed to get the User's Calendar events from our back end.
     *
     * @author Connor
     */
    @Override
    public void onStart() {
        Bundle args = getArguments();
        if (args != null) {
            mUid = (String) args.get("uuid");
            mRole = (String) args.get("role");

            AgendaTask agendaTask = new AgendaTask();

            // Gets today's date so the Agenda page Recycler View can populate with
            // events for that day from Google Calendar.
            Calendar rightNow = Calendar.getInstance();
            int year = rightNow.get(Calendar.YEAR);
            int month = rightNow.get(Calendar.MONTH);
            int dayOfMonth = rightNow.get(Calendar.DAY_OF_MONTH);
            int hour = rightNow.get(Calendar.HOUR_OF_DAY);
            int minute = rightNow.get(Calendar.MINUTE);
            agendaTask.execute(year, month, dayOfMonth, hour, minute);
        } else {
            Log.e(TAG, "Arguments were null");
        }
        super.onStart();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            mListener = (OnListFragmentInteractionListener) context;
        } catch (Exception e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }


    /**
     * This is called when the edit button is clicked on any of the cards. It sends it back to
     * the AgendaActivity and that will move to the EventFragment.
     * @param studentId
     * @param title
     * @param date
     * @param gCalId
     * @param eventId
     * @param startTime
     * @param endTime
     * @param summary
     * @param tasks
     * @author Connor
     */
    @Override
    public void onEditButtonInteraction(String studentId, String title, String date, String gCalId,
                                        String eventId, String startTime, String endTime, String summary,
                                        String[] tasks) {
        mListener.onListFragmentInteraction(studentId, title, date, gCalId, eventId, startTime, endTime, summary, tasks);
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(String studentId, String title, String date,String gCalId, String eventId, String startTime, String endTime, String summary, String[] tasks);
    }


    /**
     * This inner class will get all of the events for the current day and display it for the user
     * via the AgendaFragment's RecyclerView.
     *
     * @author Connor
     */
    public class AgendaTask extends AsyncTask<Integer, Integer, ArrayList<String>> {

        ProgressDialog dialog;


        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(getContext());
            dialog.setIndeterminate(true);
            dialog.setMessage("Getting today's events...");
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }

        @Override
        protected ArrayList<String> doInBackground(Integer... integers) {
            String response = "";
            try {

                String[] dates = DateUtil.getWholeDayStartEnd(integers[0], integers[1], integers[2]);

                String uid = mUid;
                Uri uri;
                // http://learner-backend.herokuapp.com/teacher/events?start=someTime&end=someTime&uuid=UUID
                if (mRole.equals(ChooseRoleFragment.IS_TEACHER)) {
                    uri = new Uri.Builder()
                            .scheme("http")
                            .authority("learner-backend.herokuapp.com")
                            .appendEncodedPath("teacher") //this will need to have a role check for teacher or student
                            .appendEncodedPath("events")
                            .appendQueryParameter("uuid", uid) //pass uid here
                            .appendQueryParameter("start", dates[0])
                            .appendQueryParameter("end", dates[1])
                            .build();
                } else {
                     uri = new Uri.Builder()
                            .scheme("http")
                            .authority("learner-backend.herokuapp.com")
                            .appendEncodedPath("student")
                            .appendEncodedPath("events")
                            .appendQueryParameter("uuid", uid)
                            .appendQueryParameter("start", dates[0])
                            .appendQueryParameter("end", dates[1])
                            .build();
                }


                HttpURLConnection connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                Scanner s = new Scanner(connection.getInputStream());
                StringBuilder sb = new StringBuilder();
                while(s.hasNext()) sb.append(s.next()).append(" ");
                response = sb.toString();
                JSONArray events;
                try {
                    events = new JSONObject(response)
                            .getJSONArray("events");
                } catch(JSONException e) {
                    events = new JSONArray(response);
                }
                ArrayList<String> dataset = new ArrayList<String>();
                for (int i = 0; i < events.length(); i++) {
                    String event = events.getString(i);
                    dataset.add(event);
                }
                return dataset;

            } catch (Exception e) {
                ArrayList<String> msg = new ArrayList<String>();
                Log.e(TAG, e.getMessage(), e);
                return msg;
            }
        }


        /**
         * Checks if the result ArrayList<String> from doInBackground is empty,
         * if so, then set the "empty_agenda" TextView to Visible and let the user know.
         * Otherwise, set up the RecyclerView and populate it with values from result using
         * the AgendaAdapter.
         *
         * @param result An ArrayList<String> that will never be null, but can be empty.
         * @author Connor
         */
        @Override
        protected void onPostExecute(ArrayList<String> result) {
            TextView empty = (TextView) getActivity().findViewById(R.id.empty_agenda);
            if (!result.isEmpty()) {
                if (empty != null && empty.getVisibility() != View.GONE) { //in case "empty_agenda" is already visible, make it gone.
                    empty.setVisibility(View.GONE);
                }
                mRecyclerView.setHasFixedSize(true); //change this to false if size doesn't look correct

                LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
                mRecyclerView.setLayoutManager(layoutManager);
                RecyclerView.Adapter adapter;
                adapter = new AgendaAdapter(result, getFragment());
                mRecyclerView.setAdapter(adapter); //this acts as both a set and execute.
                dialog.dismiss();
            } else {
                empty.setVisibility(View.VISIBLE);
                dialog.dismiss();
            }
        }
    }


    public AgendaFragment getFragment() {
        return this;
    }
}
