package com.example.myobothub;

import java.io.IOException;
import java.util.ArrayList;

import com.thalmic.myo.Myo;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class Connector {
	private Activity activity;
	private ArrayList<BluetoothDevice> nxts;
	private ArrayList<BluetoothSocket> sockets;
	private Myo myo;
	private String pose;
	
	// Constructeur
	public Connector(Activity activity, Myo myo) {
		this.activity = activity;
		this.myo = myo;
		
		nxts = new ArrayList<BluetoothDevice>();
		sockets = new ArrayList<BluetoothSocket>();
	}
	
	/* Fonctions */

	// Retourne la liste des Nxts connectés
	public ArrayList<BluetoothDevice> getNxts() {
		return nxts;
	}

	// Retourne le Myo associé au connecteur
	public Myo getMyo() {
		return myo;
	}
	
	// Retourne la dernière pose reçue du Myo associé au connecteur
	public String getPose() {
		return pose;
	}
	
	// Ajoute un Nxt au connecteur
	public void addNxt(BluetoothDevice nxt, BluetoothSocket socket) {
		nxts.add(nxt);
		sockets.add(socket);
	}
	
	// Ferme la connexion à un Nxt associé à un connecteur et le retire du connecteur
	public void closeNxt(BluetoothDevice nxt) {
		int i = nxts.indexOf(nxt);

		// Ferme la connexion bluetooth du Nxt
		closeSocket(sockets.get(i));
		
		// Supprime le Nxt des connexions bluetooth du connecteur
		sockets.remove(i);
		
		// Supprime le Nxt des périphériques bluetooth du connecteur
		nxts.remove(i);
	}
	
	// Ferme la connexion de tous les Nxts associés à un connecteur et les retire du connecteur
	public void closeNxts() {
		
		// Vérifie si au moins un Nxt est associé au connecteur
		if (sockets.size() > 0) {
			for (BluetoothSocket socket : sockets) {

				// Ferme la connexion bluetooth du Nxt
				closeSocket(socket);
			}

			// Réinitialise la liste des périphériques bluetooth
			nxts = new ArrayList<BluetoothDevice>();
			
			// Réinitialise la liste des connexions bluetooth
			sockets = new ArrayList<BluetoothSocket>();
		}
	}

	// Ferme la connexion bluetooth d'un Nxt associé à un connecteur
	public void closeSocket(BluetoothSocket socket) {
		try {
			socket.getOutputStream().close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			MainActivity.showError(activity, "Impossible de fermer la connexion bluetooth à " + nxts.get(sockets.indexOf(socket)).getName() + ".");
		}
	}
	
	// Envoie la dernière pose à tous les Nxts associés au connecteur
	public void sendPoseToNxts() {
		for (BluetoothSocket socket : sockets) {
			try {
				socket.getOutputStream().write(pose.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
				MainActivity.showError(activity, "Impossible d'envoyer la dernière pose à " + nxts.get(sockets.indexOf(socket)).getName() + ".");
			}
		}
	}
	
	// Modifie la pose du Myo associé au connecteur
	public void setPose(String pose) {
		this.pose = pose;
	}
}