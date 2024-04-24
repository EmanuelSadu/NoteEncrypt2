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
import java.time.LocalDateTime;
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

    public static void volleyPostNotesToServer(Context applicationContext, List<Note> notes) {
        volleyPost(applicationContext,"",  notes);
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
                    NoteDao n = Utils.getInstance().getNoteQuerryInterfce(context,null);
                    //n.increasePositions();
                    n.insertByFactor(1, jsonArray.length());
                    for(int i = 0; i < jsonArray.length(); i++){
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        Note note  =  Note.noteFRomJson(jsonObject);
                        note.setPosition(i+1);
                        jsonResponses.add(note.toString());
                        updateOrInsert(note,n);

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

    private static void updateOrInsert(Note serverNote, NoteDao n) {
       int count = n.checkIfExists(serverNote.getNoteId());
        if (count == 0 ){
            n.insert(serverNote);
            return;
        }
        Note LocalNote = n.getNoteWithNoteId(serverNote.getNoteId());

        //HACK !!! mUST bE REMOVED ONCE SERVER IF FULLY DONE !!!
        serverNote.setPosition(LocalNote.getPosition());

        LocalDateTime lastchangeServer = LocalDateTime.parse(serverNote.getTimeStamp());
        LocalDateTime lastchangeLocal = LocalDateTime.parse(LocalNote.getTimeStamp());

        // TODO Double check
        if (lastchangeServer.isAfter(lastchangeLocal)){
            serverNote.setTimeStamp(LocalNote.getTimeStamp());
            n.update(serverNote);
        }
    }


    public static void volleyPost(Context context, String request, List<Note> notes){

        List<String> jsonResponses = new ArrayList<>();
        System.out.println("Sending Voley");
        RequestQueue requestQueue = Volley.newRequestQueue(context);


        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_LOCALHOST,
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


                ObjectMapper objectMapper = new ObjectMapper();

                    try {
                        return objectMapper.writeValueAsString(notes).getBytes(StandardCharsets.UTF_8);
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
                            if (menu != null) {
                                menu.findItem(R.id.action_send).setEnabled(true);
                                menu.findItem(R.id.action_receive).setEnabled(true);
                            }

                        }
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (menu != null) {
                            menu.findItem(R.id.action_send).setEnabled(false);
                            menu.findItem(R.id.action_receive).setEnabled(false);
                        }
                        Toast.makeText(context, "No server connection", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(stringRequest);
    }
}
