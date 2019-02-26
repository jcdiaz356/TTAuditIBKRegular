package com.dataservicios.ttauditibkregular.view;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import com.dataservicios.ttauditibkregular.R;
import com.dataservicios.ttauditibkregular.adapter.ImageAdapter;
import com.dataservicios.ttauditibkregular.model.Media;
import com.dataservicios.ttauditibkregular.repo.MediaRepo;
import com.dataservicios.ttauditibkregular.util.AuditUtil;
import com.dataservicios.ttauditibkregular.util.BitmapLoader;
import com.dataservicios.ttauditibkregular.util.GlobalConstant;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class AndroidCustomGalleryActivity extends AppCompatActivity {

    public static final String LOG_TAG = AndroidCustomGalleryActivity.class.getSimpleName();
    private final int STORAGE_CODE = 100;
    private final int TAKE_PICTURE = 1;
    private final int DELETE_FILES = 1;
    private final int COPY_FILES = 2;

    private ImageAdapter imageAdapter;
    private ArrayList<Media> mediasCamera= new ArrayList<Media>();
    private Activity activity =  this;
    private String imageFileNameNew ;
    private GridView imagegrid;
    private ProgressDialog pDialog;

    private int operation; // 1= delete files , 2= copy files

    private Media                   media;
    private MediaRepo mediaRepo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_custom_gallery);
        mediasCamera = new ArrayList<Media>();

        Bundle bundle = getIntent().getExtras();
        media= new Media();
        media.setStore_id(bundle.getInt("store_id"));
        media.setPoll_id(bundle.getInt("poll_id"));
        media.setCompany_id(bundle.getInt("company_id"));
        media.setPublicity_id(bundle.getInt("publicities_id"));
        media.setProduct_id(bundle.getInt("product_id"));
        media.setCategory_product_id(bundle.getInt("category_product_id"));
        media.setMonto(bundle.getString("monto"));
        media.setRazonSocial(bundle.getString("razon_social"));
        media.setType(bundle.getInt("tipo"));
        media.setStatus(0);



        mediaRepo = new MediaRepo(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermission()) {
                //do your work
                getFromSdcard();
            } else {
                requestPermission();
            }
        }

        imagegrid = (GridView) findViewById(R.id.PhoneImageGrid);
        imageAdapter = new ImageAdapter(activity,mediasCamera);
        imagegrid.setAdapter(imageAdapter);

        Button btn_photo = (Button)findViewById(R.id.take_photo);
        Button btn_upload = (Button)findViewById(R.id.save_images);
        Button btDeletePhoto = (Button) findViewById(R.id.btDeletePhoto);
        pDialog = new ProgressDialog(activity);


        btn_photo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                // create intent with ACTION_IMAGE_CAPTURE action
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // Crea el nombre de la foto
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                imageFileNameNew =   String.format("%06d", Integer.parseInt(String.valueOf(media.getStore_id())))+ "_" + String.valueOf(media.getCompany_id()) + GlobalConstant.JPEG_FILE_PREFIX + timeStamp + GlobalConstant.JPEG_FILE_SUFFIX;
                File albumF = BitmapLoader.getAlbumDir(activity);
                // Ruta donde  se Guardara la foto en la ruta especificada
                File file = new File(albumF,imageFileNameNew);
                Uri photoPath = Uri.fromFile(file);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Uri contentUri = FileProvider.getUriForFile(activity, "com.dataservicios.ttauditibkregular.fileProvider", file);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
                } else {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoPath);
                }
                startActivityForResult(intent, TAKE_PICTURE);
            }
        });

        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                operation = COPY_FILES ;
                if (imageAdapter.countCheckedItems() > 0 ){
                     new operationFile().execute();
                }  else {
                    Toast.makeText(activity,R.string.message_selected_photo,Toast.LENGTH_SHORT).show();
                }
            }
        });

        btDeletePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                operation = DELETE_FILES ;
                if (imageAdapter.countCheckedItems() > 0 ) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(R.string.message_delete_file);
                    builder.setMessage(R.string.message_delete_file_information);
                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();
                            new operationFile().execute();


                        }
                    });
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.show();
                    builder.setCancelable(false);
                }

                else {
                    Toast.makeText(activity, R.string.message_selected_photo, Toast.LENGTH_SHORT).show();
                    return;
                }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case TAKE_PICTURE: {
                if (resultCode == RESULT_OK) {
                    // handleBigCameraPhoto();
                    //getFromSdcard();
                    Media media = new Media();
                    media.setName(imageFileNameNew);
                    media.setPathFile(BitmapLoader.getAlbumDir(activity).getAbsolutePath() + "/" + imageFileNameNew);
                    media.setSelectedFile(false);
                    mediasCamera.add(media);
                    imageAdapter.notifyDataSetChanged();
                   // imagegrid.setAdapter(imageAdapter);
                }
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case STORAGE_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do your work
                    getFromSdcard();
                } else {
                    Log.e(LOG_TAG, "Permission Denied, You cannot use local drive .");
                    finish();
                }
                break;
        }
    }

    protected boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    protected void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, R.string.message_permission_storage_local, Toast.LENGTH_LONG).show();
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.setData(Uri.parse("package:" + getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(i);
            finish();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_CODE);
            }
        }
    }

    //Enviar a AgenteDetailActivity
    public void getFromSdcard()
    {
        File file= BitmapLoader.getAlbumDir(activity);
        File[] listFileAll;
        mediasCamera.clear();
        if (file.isDirectory())
        {
            listFileAll = file.listFiles();
            if (listFileAll != null){
                for (int i = 0; i < listFileAll.length; i++)
                {
                    Media media = new Media();
                    media.setPathFile(listFileAll[i].getAbsolutePath());
                    media.setName(listFileAll[i].getName());
                    media.setSelectedFile(false);

                    mediasCamera.add(media);
                }
            }
        }
    }

    class operationFile extends AsyncTask<Void, Integer , Boolean> {
        @Override
        protected void onPreExecute() {
            //tvCargando.setText("Cargando ProductDetail...");
            pDialog = new ProgressDialog(activity);
            pDialog.setMessage(getString(R.string.message_copy_file));
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
            super.onPreExecute();

        }
        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO Auto-generated method stub
            ArrayList<Media> mediaAll;
            mediaAll =  imageAdapter.getAllMedias();
            int count = mediaAll.size();
            for (int i=0 ; i < count; i++) {
                if(mediaAll.get(i).getSelectedFile()){
                    File fileSelected;
                    fileSelected = new File(mediasCamera.get(i).getPathFile().toString());
                    if(operation == COPY_FILES ) {
                        try {
                            BitmapLoader.copyFile(fileSelected.getPath(), BitmapLoader.getAlbumDirBackup(activity).getAbsolutePath() + "/" + fileSelected.getName());
                            BitmapLoader.compresFileDestinationtion(BitmapLoader.getAlbumDirTemp(activity).getAbsolutePath(), fileSelected,activity);

                            String created_at = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(new Date());
                            media.setCreated_at(created_at);
                            media.setFile(fileSelected.getName());
                            mediaRepo.create(media);
                            fileSelected.delete();

                        } catch (IOException e) {
                            e.printStackTrace();
                            // pDialogHide();
                            //  alertDialogBasico(getString(R.string.message_no_save_photo));
                            return false;
                        }
                    } else if (operation == DELETE_FILES){
                        fileSelected.delete();
                    }
                }
            }
            //pDialogHide();
            return true;
        }
        protected void onPostExecute(Boolean result) {
            // dismiss the dialog once product deleted
            if (result){
                getFromSdcard();
                imageAdapter.notifyDataSetChanged();
                Toast.makeText(activity , R.string.message_selected_copy_photo , Toast.LENGTH_LONG).show();
                if(operation == COPY_FILES ) {
                    finish();
                }
            } else {
                Toast.makeText(activity , R.string.message_no_save_photo , Toast.LENGTH_LONG).show();
            }
            pDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }



}

