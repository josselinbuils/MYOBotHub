package com.example.myobothub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Myo.VibrationType;
import com.thalmic.myo.Pose;

public class MainActivity extends Activity {
	private Activity activity = this;
	private ArrayList<Connector> connectors;
	private BluetoothAdapter BTAdapter;
	private Button addMyoButton, addNxtButton;
	private ExpandableListView listConnectors;
	private MyExpandableListAdapter adapter;
	private Hub hub;
	private TextView stateView;

	/* Évènements de l'application */
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		stateView = (TextView) findViewById(R.id.stateView);

		// Bouton "Ajouter un Myo"
		addMyoButton = (Button) findViewById(R.id.addMyoButton);
		addMyoButton.setOnClickListener(addMyoButtonClickListener);
		
		// Bouton "Ajouter un Nxt"
		addNxtButton = (Button) findViewById(R.id.addNxtButton);
		addNxtButton.setEnabled(false);
		addNxtButton.setOnClickListener(addNxtButtonClickListener);

		// Liste des connecteurs
		listConnectors = (ExpandableListView) findViewById(R.id.listConnectors);
		
		// Supprimmer un connecteur/nxt lors d'un appui long sur un item de liste
		listConnectors.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Connector connector = null;
				BluetoothDevice nxt = null;
				String item = null;
				int i = 0;
				int group = 0;
				
				// Récupère les informations sur le connecteur/nxt
				mainLoop:
				for (Connector c : connectors) {
					if (i == position) {
						connector = c;
						item = c.getMyo().getName();
						break;
					} else {
						i++;

						for (BluetoothDevice d : c.getNxts()) {
							if (i == position) {
								connector = c;
								nxt = d;
								item = d.getName();
								break mainLoop;
							}
							
							i++;
						}
					}

					group++;
				}
				
				final Connector dConnector = connector;
				final BluetoothDevice dNxt = nxt;
				final int dGroup = group;
				
				/* Affiche une boîte de dialogue pour confirmer la suppression */
				
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);

				builder.setTitle("Supprimmer");
				builder.setMessage("Voulez-vous supprimmer " + item + " ?");

				builder.setNegativeButton("Oui", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (dConnector != null) {
							
							/* Supression du connecteur/nxt */
							
							if (dNxt != null) {
								
								// Déconnecte et supprime le Nxt
								dConnector.closeNxt(dNxt);
								
								// Met à jour la liste des connecteurs
								updateConnectorsList();
								listConnectors.expandGroup(dGroup);

							} else {
								
								// Déconnecte le Myo
								hub.detach(dConnector.getMyo().getMacAddress());
								
								// Déconnecte les Nxts reliés au Myo
								dConnector.closeNxts();
								
								// Supprime le connecteur
								connectors.remove(dConnector);
								
								// Met à jour la liste des connecteurs
								updateConnectorsList();
							}
						}
					}
				});

				builder.setPositiveButton("Non", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {}
				});

				builder.show();
				
				return true;
			}
        });

		// Initialise la liste contenant les connecteurs
		connectors = new ArrayList<Connector>();
		
		// Récupère l'adaptateur bluetooth par défaut du périphérique
		BTAdapter = BluetoothAdapter.getDefaultAdapter();

		// Vérifie si un adaptateur bluetooth a été trouvé sur le périphérique
		if (BTAdapter != null) {

			// Vérifie si le bluetooth est activé sur le périphérique
			if (!BTAdapter.isEnabled()) {
				// Active le bluetooth sur le périphérique
				BTAdapter.enable();

				// Attend que le bluetooth soit activé
				while (!BTAdapter.isEnabled());
			}
		}

		hub = Hub.getInstance();
		
		// Initialisation du HUB de gestion des Myos
		if (!hub.init(activity)) {
		    finish();
		    return;
		} else {
			hub.addListener(mListener);
		}
	}
	
	@Override
    protected void onDestroy() {
		for (Connector connector : connectors) {
			
			// Déconnecte le Myo
			hub.detach(connector.getMyo().getMacAddress());
			
			// Ferme la connexion de tous les Nxts associés au connecteur et les retire du connecteur
			connector.closeNxts();
		}
		
		// Ferme le Hub de gestion des Myos
		Hub.getInstance().shutdown();
		
        super.onDestroy();
    }
	
	/* Focntions */
	
	// Convertit une liste CharSequence en tableau CharSequence
	private CharSequence[] listToArray(ArrayList<CharSequence> list) {
	    CharSequence[] sequence = new CharSequence[list.size()];
	    
	    for (int i = 0; i < list.size(); i++) {
	        sequence[i] = list.get(i);
	    }
	    
	    return sequence;
	}
	
	// Retourne le connecteur auquel appartient un Myo
	private Connector getConnector(Myo myo) {
		Connector connector = null;
		
		if (connectors.size() > 0) {
			for (Connector c : connectors) {
				if (c.getMyo() == myo) {
					connector = c;
					break;
				}
			}
		}
		
		return connector;
	}
	
	// Affiche une erreur à l'aide d'une boîte de dialogue
	public static void showError(Activity activity, String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle("Erreur");
		builder.setMessage(msg);
		builder.setIcon(android.R.drawable.ic_dialog_alert);

		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) { }
		});
		
		builder.show();
	}
	
	// Met à jour la liste des connecteurs (ListView)
	private void updateConnectorsList() {
		SparseArray<Group> groups = new SparseArray<Group>();
		
		if (connectors.size() > 0) {
			
			int i = 0;
	
			for (Connector connector : connectors) {
				Group group = new Group(connector.getMyo().getName());
				
				ArrayList<BluetoothDevice> nxts = connector.getNxts();
	
				if (nxts != null && nxts.size() > 0) {
					for (BluetoothDevice nxt : connector.getNxts()) {
						group.children.add(nxt.getName());
					}
				}
		
				groups.append(i, group);
	
				i++;
			}
			
			addNxtButton.setEnabled(true);
		} else {
			addNxtButton.setEnabled(false);
		}

		adapter = new MyExpandableListAdapter(this, groups);
		listConnectors.setAdapter(adapter);
	}
	
	/* Écouteurs */
	
	// Écouteur des MYO
	private DeviceListener mListener = new AbstractDeviceListener() {
		@Override
		public void onConnect(Myo myo, long timestamp) {
			// Crée un connecteur
			connectors.add(new Connector(activity, myo));
			
			// Met à jour la liste des connecteurs
			updateConnectorsList();
			
			// Affichage de l'état
			stateView.setTextColor(Color.rgb(40, 146, 194));
			stateView.setText("État : " + myo.getName() + " connecté");
			
			// Fait vibrer le Myo pour indiquer la connexion
			myo.vibrate(VibrationType.SHORT);
		}

		@Override
		public void onDisconnect(Myo myo, long timestamp) {
			Connector connector = getConnector(myo);

			if (connector != null) {
				// Déconnecte les Nxts reliés au Myo
				connector.closeNxts();
				
				// Affichage de l'état
				stateView.setTextColor(Color.RED);
				stateView.setText("État : " + myo.getName() + " déconnecté");
	
				// Supprime le connecteur
				connectors.remove(connector);
			}
		}

		@Override
		public void onPose(Myo myo, long timestamp, Pose pose) {
			// Récupère le connecteur du Myo
			Connector connector = getConnector(myo);

			// Vérifie que le connecteur existe
			if (connector != null) {
				
				/* Stocke la pose dans un string */
				
				String p = null;

				switch (pose) {
				case FINGERS_SPREAD:
					p = "FINGERS_SPREAD";
					break;
				case FIST:
					p = "FIST";
					break;
				case REST:
					p = "REST";
					break;
				case DOUBLE_TAP:
					p = "DOUBLE_TAP";
					break;
				case UNKNOWN:
					p = "UNKNOWN";
					break;
				case WAVE_IN:
					p = "WAVE_IN";
					break;
				case WAVE_OUT:
					p = "WAVE_OUT";
					break;
				}
				
				// Affichage de l'état
				stateView.setTextColor(Color.rgb(40, 146, 194));
				stateView.setText("État : " + myo.getName() + " -> " + p);

				// Définie la nouvelle pose dans le connecteur
				connector.setPose(p);
				
				// Envoie la pose à tous les Nxts associés au connecteur
				if (connector.sendPoseToNxts() == false) { // Erreur de communication
					updateConnectorsList();
				}
			}
		}
	};

	// Écouteur du boutton "Ajouter un Myo"
	private OnClickListener addMyoButtonClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Affichage de l'état
			stateView.setTextColor(Color.rgb(40, 146, 194));
			stateView.setText("État : connexion au myo...");
			
			// Appairage avec le MYO le plus proche
			hub.attachToAdjacentMyo();
		}
	};
	
	// Écouteur du boutton "Ajouter un Nxt"
	private OnClickListener addNxtButtonClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {

			// Récupère la listes des périphériques bluetooth liés au périphérique
			final Set<BluetoothDevice> bondedDevices = BTAdapter.getBondedDevices();

			// Vérifie que des périphériques bluetooth sont liés au périphérique
			if (bondedDevices.size() > 0) {
				
				// Crée une boîte de sélection pour sélectionner un Myo
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				
				// Fixe le titre de la boîte de sélection
				builder.setTitle("Sélectionnez un Myo");
				
				/* Génère les items de la boîte de sélection */

				final ArrayList<Myo> myos = new ArrayList<Myo>();
				ArrayList<CharSequence> myoNames = new ArrayList<CharSequence>();
				
				for (Connector connector : connectors) {
					myos.add(connector.getMyo());
					myoNames.add(connector.getMyo().getName());
				}
				
				// Fixe les items de la boîte de sélection et leur écouteur
				builder.setItems(listToArray(myoNames), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final Connector connector = getConnector(myos.get(which));
						final int groupPosition = which;
						
						// Crée une boîte de sélection pour sélectionner un NXT
						AlertDialog.Builder builder = new AlertDialog.Builder(activity);
						
						// Fixe le titre de la boîte de sélection
						builder.setTitle("Sélectionnez un NXT");
						
						/* Génère les items de la boîte de sélection */

						ArrayList<CharSequence> bondedDevicesNames = new ArrayList<CharSequence>();
						final ArrayList<BluetoothDevice> bondedDevicesList = new ArrayList<BluetoothDevice>();
						
						// Ajoute à la liste les péripériques qui n'ont pas déjà été ajoutés à l'application
						for (BluetoothDevice device : bondedDevices) {
							boolean flag = true;

							// Vérifie si le périphérique a déjà été ajouté à l'application
							for (BluetoothDevice nxt : connector.getNxts()) {
								if (device.getAddress().equals(nxt.getAddress())) {
									flag = false;
									break;
								}
							}
							
							// Si ce n'est pas le cas, on l'ajoute à la liste
							if (flag == true) {
								bondedDevicesNames.add(device.getName());
								bondedDevicesList.add(device);
							}
						}
						
						// Fixe les items de la boîte de sélection et leur écouteur
						builder.setItems(listToArray(bondedDevicesNames), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								BluetoothDevice nxt = bondedDevicesList.get(which);
								BluetoothSocket socket = null;
								
								// Affichage de l'état
								stateView.setTextColor(Color.rgb(40, 146, 194));
								stateView.setText("État : connexion à " + nxt.getName() + "...");

								try {
									// Tente de se connecter au Nxt
									socket = nxt.createRfcommSocketToServiceRecord(nxt.getUuids()[0].getUuid());
									socket.connect();
									
									// Ajoute le Nxt au connecteur
									connector.addNxt(nxt, socket);
									
									// Affichage de l'état
									stateView.setTextColor(Color.rgb(40, 146, 194));
									stateView.setText("État : " + nxt.getName() + " connecté");
									
									// Met à jour la liste des connecteurs
									updateConnectorsList();
									listConnectors.expandGroup(groupPosition);
								
								} catch (IOException e) {
									e.printStackTrace();
									showError(activity, "Impossible de se connecter à " + nxt.getName() + ", vérifiez que le bluetooth est activé sur le NXT et qu'il est appairé avec cet appareil.");
								}
							}
						});
						
						// Affiche la boîte de sélection
						builder.show();
					}
				});
				
				// Affiche la boîte de sélection
				builder.show();
			}
		}
	};
}