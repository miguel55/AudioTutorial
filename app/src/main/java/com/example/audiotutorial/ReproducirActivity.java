package com.example.audiotutorial;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;


public class ReproducirActivity extends Activity implements View.OnClickListener{
    private Button play, stop;
    private File archivo;
    private Context context;
    boolean reproduciendo=false;
    int freq = 44100, confCanales = AudioFormat.CHANNEL_OUT_MONO,codificacion = AudioFormat.ENCODING_PCM_16BIT;
    Reproduce tareaRep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reproducir);
        context=this.getBaseContext();
        // Se obtiene el archivo que se pasa en el Intent
        Bundle bundle = getIntent().getExtras();
        archivo=new File(bundle.getString("file"));

        // Se instancian los botones
        play = (Button) findViewById(R.id.button3);
        stop = (Button) findViewById(R.id.button4);

        // Se crea un evento ClickListener para cada botón
        play.setOnClickListener(this);
        stop.setOnClickListener(this);

        // Se activan y desactivan los botones
        play.setEnabled(true);
        stop.setEnabled(false);
    }

    @Override
    public void onClick(View v) {
        if (v==play){
            playS();
        } else if (v==stop){
            stopS();
        }
    }

    public void playS () {
        reproduciendo=true;
        play.setEnabled(false);
        stop.setEnabled(true);
        // Se inicia la tarea asíncrona de la reproducción
        tareaRep=new Reproduce();
        tareaRep.execute();
    }

    public void stopS () {
        // Se modifica el valor de la variable reproducción para detener la reproducción
        reproduciendo=false;
        play.setEnabled(true);
        stop.setEnabled(false);
    }

    private class Reproduce extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // Se obtiene el tamaño mínimo de buffer
            int tam = AudioTrack.getMinBufferSize(freq,confCanales, codificacion);
            short[] audiodata = new short[tam];

            try {
                // Se genera el flujo de entrada para el fichero
                DataInputStream data = new DataInputStream(new BufferedInputStream(new FileInputStream(archivo)));
                // Se llama al constructor de la clase
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, freq, confCanales,
                        codificacion, tam,AudioTrack.MODE_STREAM);

                // Se inicia la reproducción
                audioTrack.play();
                while (reproduciendo && data.available() > 0) {
                    // Mientras no se haya detenido la reproducción y queden datos en el fichero,
                    // se continúa leyendo muestras y reproduciendo
                    int i = 0;
                    while (data.available() > 0 && i < audiodata.length) {
                        audiodata[i] = data.readShort();
                        i++;
                    }
                    audioTrack.write(audiodata, 0, audiodata.length);
                }
                // Se libera el objeto para otras reproducciones
                audioTrack.stop();
                audioTrack.release();
                data.close();
            } catch (Throwable t) {
                Log.e("AudioTrack", "Fallo en la reproducción");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            play.setEnabled(false);
            stop.setEnabled(false);
            // Se elimina el fichero para no ocupar memoria
            if (archivo.delete())
                Toast.makeText(context, "El fichero se eliminó correctamente", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(context, "No se pudo eliminar el fichero", Toast.LENGTH_LONG).show();
            // Se retorna a la actividad principal
            Intent intent=new Intent(context,GrabarActivity.class);
            startActivity(intent);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
