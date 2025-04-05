package com.example.retroplay.Viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.retroplay.Repository.UsuarioRepository;

import java.io.Closeable;


public class UsuarioViewModel extends AndroidViewModel {
    private final UsuarioRepository usuarioRepository;
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> updateSuccess = new MutableLiveData<>();

    public UsuarioViewModel(@NonNull Application application) {
        super(application);
        this.usuarioRepository = new UsuarioRepository(application);
    }


    public LiveData<String> getUserName(String userId) {
        isLoading.setValue(true);
        MutableLiveData<String> userNameLiveData = new MutableLiveData<>();

        usuarioRepository.getUserName(userId).observeForever(name -> {
            isLoading.setValue(false);
            if (name == null) {
                errorMessage.setValue("Error al cargar datos del usuario");
            } else {
                userNameLiveData.setValue(name);
            }
        });

        return userNameLiveData;
    }

    public void updateUser(String userId, String currentEmail, String currentPassword,
                           String newName, String newPassword) {
        isLoading.setValue(true);

        usuarioRepository.reauthenticateUser(currentEmail, currentPassword).observeForever(isAuthenticated -> {
            if (isAuthenticated) {
                usuarioRepository.updateUserData(userId, newName, newPassword).observeForever(isUpdated -> {
                    isLoading.setValue(false);
                    if (isUpdated) {
                        updateSuccess.setValue(true);
                    } else {
                        errorMessage.setValue("Error al actualizar datos del usuario");
                    }
                });
            } else {
                isLoading.setValue(false);
                errorMessage.setValue("Contraseña actual incorrecta");
            }
        });
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getUpdateSuccess() {
        return updateSuccess;
    }

    public void signOut() {
        usuarioRepository.signOut();
    }
}
