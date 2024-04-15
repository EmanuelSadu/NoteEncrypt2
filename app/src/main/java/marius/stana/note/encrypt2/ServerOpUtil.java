package marius.stana.note.encrypt2;

import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerOpUtil {

    private ServerOpUtil serverOpUtil;

    private ServerOpUtil(){

    }

    public ServerOpUtil getInstance() {
        if (serverOpUtil == null)
            serverOpUtil =  new ServerOpUtil();
        return serverOpUtil;
    }

    private static final String URL_LOCALHOST ="http:/10.0.2.2:7878" ;
    private static NoteDao noteQueryInterface ;
    private static final String URL ="https://reqres.in/api/users?" ;

    public static void checkServer(Context applicationContext, Menu menu) {
        //TO-DO Check healthstatus of my server
         volleyGetStatus(applicationContext, Request.Method.GET,"" ,menu);
    }

    public static void volleyGetNotesFromServer(Context applicationContext) {
        volleyGet(applicationContext, "", null);
    }

    public static void volleyPostNotesToServer(Context applicationContext) {
        volleyPost(applicationContext,"", null);
    }


    public static void volleyGet(Context context, String request, Menu menu){

        List<String> jsonResponses = new ArrayList<>();
        System.out.println("Sending Voley");
        RequestQueue requestQueue = Volley.newRequestQueue(context);


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL_LOCALHOST+request, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("notes");
                    for(int i = 0; i < jsonArray.length(); i++){
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        Note note  =  Note.noteFRomJson(jsonObject);
                        jsonResponses.add(note.toString());
                        NoteDao n = Utils.getInstance().getNoteQuerryInterfce(context,null);
                        Toast.makeText(context, note.toString(), Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }


    public static void volleyPost(Context context, String request, Menu menu){

        List<String> jsonResponses = new ArrayList<>();
        System.out.println("Sending Voley");
        RequestQueue requestQueue = Volley.newRequestQueue(context);


        StringRequest stringRequest = new StringRequest(Request.Method.GET, URL_LOCALHOST,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Toast.makeText(context, "Notes have been sent to server", Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, "Notes have been sent to server", Toast.LENGTH_SHORT).show();
            }
        }){

            @Override
            public byte[] getBody()
            {

                NoteDao n = Utils.getInstance().getNoteQuerryInterfce(context,null);
                Note note = n.getFromPosition(0);
                ObjectMapper objectMapper = new ObjectMapper();

                try {
                    return objectMapper.writeValueAsString(note).getBytes(StandardCharsets.UTF_8);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                return null;
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }

    public static void volleyGetStatus(Context context, int method, String request, Menu menu){

        List<String> jsonResponses = new ArrayList<>();
        System.out.println("Sending Voley");
        RequestQueue requestQueue = Volley.newRequestQueue(context);


        StringRequest stringRequest = new StringRequest( Request.Method.GET, URL_LOCALHOST,

                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Voley", "Response");
                        if (response != null || !response.isEmpty()){
                            menu.findItem(R.id.action_send).setEnabled(true);
                            menu.findItem(R.id.action_receive).setEnabled(true);

                        }
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        menu.findItem(R.id.action_send).setEnabled(false);
                        menu.findItem(R.id.action_receive).setEnabled(false);
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                        builder1.setMessage("Connection to server Not established");
                        builder1.setCancelable(true);

                        builder1.setPositiveButton(
                                "Ok",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alert11 = builder1.create();
                        alert11.show();
                        error.printStackTrace();
                    }
                }
        );

        requestQueue.add(stringRequest);
    }
}
