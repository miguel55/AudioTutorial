package com.example.audiotutorial;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class GrabarActivity extends Activity implements View.OnClickListener{
    private TextView estadoG;
    private Button grab, det;
    private File archivo, path;
    private Context context;
    boolean grabando=false;
    int freq = 44100, confCanales = AudioFormat.CHANNEL_IN_MONO, codificacion = AudioFormat.ENCODING_PCM_16BIT;
    Graba tareaGrabar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grabar);
        context=this.getBaseContext();

        // Se instancian los botones y el texto de estado
        estadoG = (TextView) findViewById(R.id.statusGtxt);
        grab = (Button) findViewById(R.id.button1);
        det = (Button) findViewById(R.id.button2);

        // Se crea un evento ClickListener para cada botón
        grab.setOnClickListener(this);
        det.setOnClickListener(this);

        // Se activan y desactivan los botones
        grab.setEnabled(true);
        det.setEnabled(false);

        // Se crea el archivo para la grabación
        path=new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/");
        path.mkdir();
        try {
            archivo = File.createTempFile("grab", ".pcm", path);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create file on SD card", e);
        }
    }

    @Override
    public void onClick(View v) {
        if (v==grab){
            grabar();
        } else if (v==det){
            detener();
        }
    }

    public void grabar() {
        grab.setEnabled(false);
        det.setEnabled(true);
        grabando=true;
        // Se ejecuta la tarea asíncrona de grabación
        tareaGrabar= new Graba();
        tareaGrabar.execute();
    }

    public void detener () {
        // Se cambia el valor de la variable grabando para detener la grabación
        grabando=false;
        grab.setEnabled(true);
        det.setEnabled(false);
    }

    private class Graba extends AsyncTask<Void, Double, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // Se obtiene el tamaño mínimo del buffer necesario
            int tam = AudioRecord.getMinBufferSize(freq,confCanales, codificacion);
            short[] buffer = new short[tam];

            try {
                // Se crea el flujo de datos para escribir en el fichero
                DataOutputStream data = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(archivo)));
                // Se llama al constructor de la clase AudioRecord
                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, freq, confCanales, codificacion, tam);

                // Comienza el proceso de grabación
                audioRecord.startRecording();
                double progress = 0;
                while (grabando) {
                    // Mientras se desee seguir grabando, se toman las muestras leyendo del micrófono
                    // y se almacenan en el buffer
                    int muestras = audioRecord.read(buffer, 0,tam);
                    for (int i = 0; i < muestras; i++) {
                        data.writeShort(buffer[i]);
                    }
                    // Se publica el progreso
                    publishProgress(new Double(progress));
                    progress = progress + (double) (freq/muestras);
                }
                audioRecord.stop();
                // Se libera el objeto para poder reproducir y grabar tantas veces como sea necesario
                audioRecord.release();
                data.close();
            } catch (Throwable t) {
                Log.e("AudioRecord", "Fallo en la grabación");
            }
            return null;
        }

        // Para publicar el progreso
        @Override
        protected void onProgressUpdate(Double... progress) {
            estadoG.setText(progress[0].toString());
        }

        // Se pasa a la actividad de reproducción
        @Override
        protected void onPostExecute(Void result) {
            Intent intent=new Intent(context,ReproducirActivity.class);
            intent.putExtra("file",archivo.toString());
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
