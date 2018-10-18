# CaenUhfSample
Sample application for Caen UHF reader on C-One

## Prerequisites

CpcSystemServices shall be installed on your device.

Please install the last version available on FDroid available on www.coppernic.fr/fdroid.apk

## Set up

### build.gradle

```groovy
repositories {
    jcenter()
    maven { url 'https://artifactory.coppernic.fr/artifactory/libs-release' }
}


dependencies {
// [...]
     // Coppernic
    implementation('fr.coppernic.sdk.cpcutils:CpcUtilsLib:6.17.0')
    implementation('fr.coppernic.sdk.core:CpcCore:1.6.2')
    implementation 'com.caen.RFIDLibrary:CaenRfidLibrary:3.0.0'
// [...]
}

```

### Power management

 * Implements power listener

```java

  private final PowerListener powerListener = new PowerListener() {
        @Override
        public void onPowerUp(CpcResult.RESULT result, Peripheral peripheral) {
            if (result == CpcResult.RESULT.OK) {
                //Caen UHF reader is ON
            }
            else{
                //Error while powering Caen UHF
            }
        }

        @Override
        public void onPowerDown(CpcResult.RESULT result, Peripheral peripheral) {
           //Caen UHF reader power off
        }
    };

```

 * Register the listener

```java
@Override
    protected void onStart() {
// [...]
        PowerManager.get().registerListener(powerListener);
// [...]
    }
```

 * Power reader on

```java
// Powers on Fingerprint reader
ConePeripheral.RFID_CAEN_R1270C_GPIO.on(this);
// The listener will be called with the result
```

 * Power off when you are done

```java
// Powers off OCR reader
ConePeripheral.RFID_CAEN_R1270C_GPIO.off(this);
// The listener will be called with the result
```

 * unregister listener resources

```java
@Override
    protected void onStop() {
// [...]
        PowerManager.get().unregisterListener(powerListener);
// [...]
    }
```

### Reader initialization

#### Create reader object
 * Get reader instance

```java
    private CAENRFIDReader reader;
    ...
    
    if (reader == null) {
            CAENRFIDReader.getInstance(context, new OnGetReaderInstanceListener() {
                @Override
                public void OnGetReaderInstance(CAENRFIDReader caenrfidReader) {
                    reader = caenrfidReader;
                }
            });
        }
```
 * Connect to the reader 
 
```java
     try {
            reader.Connect(CAENRFIDPort.CAENRFID_RS232, CAEN_READER_PORT);
        } catch (CAENRFIDException e) {
            e.printStackTrace();
            return CpcResult.RESULT.CONNECTION_ERROR;
        }
    
```
 * Create listener to get tag result

```java

  private CAENRFIDEventListener caenrfidEventListener = new CAENRFIDEventListener() {
        @Override
        public void CAENRFIDTagNotify(CAENRFIDEvent caenrfidEvent) {
            Timber.d("Tag received!!");
            ArrayList<CAENRFIDNotify> tags = caenrfidEvent.getData();
        }
    };

```

### Start inventory

 * When your reader is initialized and connected. Start inventory
 
```java
    Timber.d("Start inventory");
    CAENRFIDLogicalSource source = reader.GetSource("Source_0");
    source.SetReadCycle(0);
    reader.addCAENRFIDEventListener(caenrfidEventListener);
    source.SetSelected_EPC_C1G2(CAENRFIDLogicalSourceConstants.EPC_C1G2_All_SELECTED);
    source.EventInventoryTag(new byte[0], (short) 0x0, (short) 0x0, (short) 0x06);

```
 * Stop inventory
 
```java
   Timber.d("Stop inventory and disconnect reader");
   reader.removeCAENRFIDEventListener(caenrfidEventListener);
   reader.InventoryAbort();
```

 * Disconnect reader when you are done
 
 ```java
    reader.Disconnect();
 ```java
 
