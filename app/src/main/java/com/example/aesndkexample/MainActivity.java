package com.example.aesndkexample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    Context context;
    Button cameraButton;
    ImageView imageView;
    TextView textView;

    String key;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    List<Item> items;

    String gblDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        items = new ArrayList<>();
        gblDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        textView = findViewById(R.id.textViewTest);
        cameraButton = findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(this);
        recyclerView = findViewById(R.id.usersRecyclerView);

        layoutManager = new LinearLayoutManager(this);
        layoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(layoutManager);
        inicializarLista();
        //llenarAdaptador();


        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            //Verificar si hay una camara disponible
        }

    }

    private void setUpKeyDialog(String title,
                                final String msgPositive,
                                final String msgNegative,
                                String msgTextViewDescription) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(false);
        View viewInflated = LayoutInflater.from(this)
                .inflate(R.layout.fragment_put_key,
                        (ViewGroup) findViewById(android.R.id.content), false);
        final EditText input = viewInflated.findViewById(R.id.keyEditText);
        final TextView textView = viewInflated.findViewById(R.id.textViewToRequestKey);
        textView.setText(msgTextViewDescription);
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                key = input.getText().toString();
                Log.i("EncodeKey", key);

                if (key.length() <= 16 && key.length() > 0) {

                    StringBuilder sb = new StringBuilder(key);

                    while (sb.length() < 16) {
                        sb.append('*');
                    }
                    byte[] k = sb.toString().getBytes(StandardCharsets.UTF_8);

                    //Encriptar y reemplazar foto sin encriptar por la encriptada
                    File f = new File(rutaUltimaFoto);
                    File data = encripProcess(k, f);
                    data.renameTo(f);
                    updateRecycler();
                    writeToFile(key + ";" + f.getName() + "\n");
                    Toast.makeText(context, msgPositive, Toast.LENGTH_SHORT).show();
                } else {
                    // No cumple con los requisitos del formato de key
                    Toast.makeText(context, "La clave debe tener 16 caracteres o menos.", Toast.LENGTH_SHORT).show();
                    deleteLastPhoto();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(context, msgNegative, Toast.LENGTH_SHORT).show();
                //Se borra la última foto porque no hay clave de encriptacion
                deleteLastPhoto();
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void deleteLastPhoto() {
        File file = new File(rutaUltimaFoto);
        file.delete();
        updateRecycler();
    }

    private File encripProcess(byte[] k, File f) {
        try {
            //Informacion a encriptar
            Log.i("Size of F", f.getName() + " " + f.length());
            FileInputStream fileInputStream = new FileInputStream(f);
            //Archivo que va a tener los datos encriptados
            if (!new File(gblDir + "/temp").exists()) {
                File directorioTemp = new File(gblDir + "/temp/");
                directorioTemp.mkdir();
            }
            File fileEncripted = new File(gblDir + "/temp/image_enc.jpg");
            FileOutputStream fileOutputStream = new FileOutputStream(fileEncripted);

            long total = 0;
            long tam = 2000000;

            while (total < f.length()) {
                tam = (f.length() - total > tam) ? tam : f.length() - total;
                total += tam;
                byte[] dataToBeEnctrypted = new byte[(int) tam];
                fileInputStream.read(dataToBeEnctrypted);
                byte[] encryptedData = crypt(dataToBeEnctrypted, k, System.currentTimeMillis(), 0);
                System.out.println(encryptedData.length);
                fileOutputStream.write(encryptedData);
            }
            fileInputStream.close();
            fileOutputStream.close();

            Log.i("Size of F", fileEncripted.getName() + " " + fileEncripted.length());

            //Borrar imagen original (no encriptada)
            //File fileToDelete = new File(rutaUltimaFoto);
            //fileToDelete.delete();

            return fileEncripted;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private File decriptProcess(byte[] k, File f) {
        try {
            FileInputStream fise = new FileInputStream(f);
            if (!new File(gblDir + "/temp").exists()) {
                File directorioTemp = new File(gblDir + "/temp/");
                directorioTemp.mkdir();
            }
            File fd = new File(gblDir + "/temp/image_desenc.jpg");
            FileOutputStream fosd = new FileOutputStream(fd);

            long total = 0;
            long tam = 2000016;
            System.out.println("dec");
            while (total < f.length()) {
                tam = (f.length() - total > tam) ? tam : f.length() - total;
                total += tam;
                byte[] dataToBeDectrypted = new byte[(int) tam];
                fise.read(dataToBeDectrypted);
                byte[] decryptedData = crypt(dataToBeDectrypted, k, System.currentTimeMillis(), 1);
                System.out.println(decryptedData.length);
                fosd.write(decryptedData);
            }

            fise.close();
            fosd.close();

            return fd;
        } catch (Exception e) {
            Log.e("DecryptErrorFile", "Error con archivo en proceso de desencriptación");
            e.printStackTrace();
            return null;
        }
    }

    public native byte[] crypt(byte[] data, byte[] key, long time, int mode);

    private void putInKeyDialog(String title, final String msgPositive,
                                final String msgNegative,
                                final String msgTextViewDescription,
                                final String rutaArchivoADesencriptar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(false);
        View viewInflated = LayoutInflater.from(this)
                .inflate(R.layout.fragment_put_key,
                        (ViewGroup) findViewById(android.R.id.content), false);
        final EditText input = viewInflated.findViewById(R.id.keyEditText);
        final TextView textView = viewInflated.findViewById(R.id.textViewToRequestKey);
        textView.setText(msgTextViewDescription);

        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String key = input.getText().toString();

                File f = new File(rutaArchivoADesencriptar);

                if (readFromFile(key, f.getName())) {
                    StringBuilder sb = new StringBuilder(key);

                    while (sb.length() < 16) {
                        sb.append('*');
                    }

                    byte[] k = sb.toString().getBytes(StandardCharsets.UTF_8);
                    File fileToDecode = new File(rutaArchivoADesencriptar);

                    File data;


                    data = decriptProcess(k, fileToDecode);

                    Bitmap bitmap = BitmapFactory.decodeFile(data.getAbsolutePath());
                    showImageDialog(f.getName(), bitmap);
                    Toast.makeText(context, msgPositive, Toast.LENGTH_SHORT).show();

                } else {
                    //Toast.makeText(context, "No tiene 16 caracteres", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(context, msgNegative, Toast.LENGTH_SHORT).show();
                dialog.cancel();
            }
        });
        builder.show();
    }

    Bitmap bitmapToShowOnDialog;

    private void showImageDialog(String imageName, Bitmap bitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(imageName);
        View viewInflated = LayoutInflater.from(this)
                .inflate(R.layout.fragment_image_dialog,
                        (ViewGroup) findViewById(android.R.id.content), false);
        final ImageView imageView = viewInflated.findViewById(R.id.imageViewDialog);
        imageView.setImageBitmap(bitmap);

        builder.setView(viewInflated);

        builder.setNegativeButton("Cerrar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void inicializarLista() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.i("TEST", storageDir.getAbsolutePath());
        for (File f : storageDir.listFiles()) {
            if (!f.isDirectory()) {
                items.add(new Item(f.getName(), f.getAbsolutePath()));
                Log.i("Directorio", f.getAbsolutePath());
            }
        }

        mAdapter = new MyImagesAdapter(this, items);
        ((MyImagesAdapter) mAdapter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String rutaToDecode = items.get(recyclerView.getChildAdapterPosition(v)).getRutaAbsoluta();
                Log.i("RutaArchivoSeleccionado", rutaToDecode);


                putInKeyDialog("Decode Key",
                        "Success decode",
                        "Failed decode",
                        "Please, put in the key to decode the selected photo:",
                        rutaToDecode);
            }
        });
        recyclerView.setAdapter(mAdapter);
    }

    List<Item> tmp;

    private void updateRecycler() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        tmp = new ArrayList<>();
        items.clear();
        for (File f : storageDir.listFiles()) {
            if (!f.isDirectory()) {
                tmp.add(new Item(f.getName(), f.getAbsolutePath()));
            }
        }
        items.addAll(tmp);
        mAdapter.notifyDataSetChanged();
    }


    static final int REQUEST_TAKE_PHOTO = 1;

    String rutaUltimaFoto;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                rutaUltimaFoto = photoFile.getAbsolutePath();
            } catch (IOException ex) {
                Log.e("ErrorCreandoArchivo", ex.getMessage());
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.aesndkexample",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            updateRecycler();

            setUpKeyDialog("Encode Key",
                    "Success encode",
                    "Failed encode",
                    "Please, put in a Key (Size 16) to encode this photo:");
        }
    }

    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        Log.i("AbsolutePathNewImage", image.getAbsolutePath());

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

    @Override
    public void onClick(View v) {
        if (R.id.cameraButton == v.getId()) {
            dispatchTakePictureIntent();
            //final File out = new File("/storage/emulated/0/Android/data/com.example.aesndkexample" +
            //        "/files/Pictures/", "JPEG_20190510_013646_6731722165748035791.jpg");
        }
    }

    //-----------------------------------------------------

    /**
     * Archivos de la forma checksum;nombre_imagen
     *
     * @param data
     */
    private void writeToFile(String data) {
        try {
            //Validamos que el directorio exista
            if (!new File(gblDir + "/checksum").exists()) {
                File directorioTemp = new File(gblDir + "/checksum/");
                directorioTemp.mkdir();
            }
            File file = new File(gblDir + "/checksum/EstasNoSonLasLlaves.txt");
            FileWriter fileWritter = new FileWriter(file, true);
            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
            bufferWritter.write(data);
            bufferWritter.close();
            fileWritter.close();

        } catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    /**
     * Archivo para leer las llaves
     *
     * @param key      llave a comparar
     * @param filename archivo a buscar
     * @return true si las llaves son correctas
     */
    private boolean readFromFile(String key, String filename) {
        String line;
        String[] readline;
        try {
            //Validamos que el directorio exista
            if (!new File(gblDir + "/checksum").exists()) {
                File directorioTemp = new File(gblDir + "/checksum/");
                directorioTemp.mkdir();
            }

            File file = new File(gblDir + "/checksum/EstasNoSonLasLlaves.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                Log.i("Linea", "readFromFile: " + line);
                readline = line.split(";");
                if (readline[1].equals(filename)) { //lee nombre del archivo
                    if (readline[0].equals(key)) {//si la contraseñas son iguales
                        Log.i("llave", "readFromFile: " + readline[0]);
                        bufferedReader.close();
                        fileReader.close();
                        return true;
                    } else {
                        Toast.makeText(context, "Clave Incorrecta", Toast.LENGTH_SHORT).show();
                        bufferedReader.close();
                        fileReader.close();
                        return false;
                    }
                }

            }

        } catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }

        return false;
    }
}
