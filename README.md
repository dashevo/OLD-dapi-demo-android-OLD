# Android DAPI & Schema Demo
Demo of DAPI and Schema usage in an Android app using a VMN.

This demo consists of a simple contact list application.  
DAP Objects will be created using 
[Dash-Schema-Android](https://github.com/dashevo/dash-schema-android) and be sent to a 
[VMN](https://github.com/dashevo/vmn-dapi-rest) instance using
[DAPI-Client-Android](https://github.com/dashevo/dapi-client-android)

## Dependencies 
- Android JSON Schema: https://github.com/sambarboza/json-schema
- VMN Dapi Rest: https://github.com/dashevo/vmn-dapi-rest
- Dash Schema Android: https://github.com/dashevo/dash-schema-android
- Dapi Client Android: https://github.com/dashevo/dapi-client-android

## Installation
- Clone the 4 repos above in the same folder and run:
  - `npm install` inside `vmn-dapi-rest`
  - `mvn install -DskipTests` inside `json-schema`
  - `./gradlew assemble` inside `dash-schema-android` and `dapi-client-android`
  
## Running
- Before running the application, make sure that the VMN is running and set your local ip in [DapiDemoClient.kt#L18](https://github.com/dashevo/dapi-demo-android/blob/master/app/src/main/java/org/dashevo/dapidemo/dapi/DapiDemoClient.kt#L18)
- Open the main folder of this repo in Android Studio and run on device or emulator.
- To run on a device, make sure that both computer and mobile device are in the same network.

## Usage
- The first login should be made with the username "alice", so the hardcoded DAP id in the app will match with the one created by alice on the first login.
- A user will be created on its first login and a DAP Object of the type user will be sent.
- To change the current user just kill/close the app and login with a new/existing user.
- A contact can have one of these 3 states: `requested`, `pending` and `approved`, in Alice's perspective they would be:
  - `pending`: Alice added Bob as contact and it is pending of approval.
  - `requested`: Bob added Alice as a contact but Alice has not approved yet.
  - `approved`: Bob added Alice as contact and she approved.

## States and DAP Context examples
Contact states are derived based on the presence of DAP Objects containing the contact information in a user DAP Context.

#### Alice signup, no contact added:
```json
{
    "dapid": "c78a05c06876a61a3942c2e5618ceec0a51e301b2b708f908165a2c00ca32cb8",
    "buid": "9ac9f8f7082f9d78975e9e6c186c7f358ee7c6855cedea4b06b10df5d34fdabc",
    "objects": [
        {
            "act": 1,
            "objtype": "user",
            "rev": 0,
            "idx": 0,
            "aboutme": "I'm alice"
        }
    ],
    "related": null,
    "maxidx": 0
}
```
#### Alice added Bob as contact and it is waiting for approval:
##### Alice's State (Bob is in the "Pending" list):
```json
{
    "dapid": "c78a05c06876a61a3942c2e5618ceec0a51e301b2b708f908165a2c00ca32cb8",
    "buid": "9ac9f8f7082f9d78975e9e6c186c7f358ee7c6855cedea4b06b10df5d34fdabc",
    "objects": [
        {
            "act": 1,
            "objtype": "user",
            "rev": 0,
            "idx": 0,
            "aboutme": "I'm alice"
        },
        {
            "meta": {
                "sig": "1ae00a11b6ea301fa0d7e42b8c02af5bb9f5533aee824d16abe4950071031d14"
            },
            "act": 1,
            "objtype": "contact",
            "rev": 0,
            "idx": 1,
            "user": {
                "type": 0,
                "userId": "c5a653f70aac16e8a99baa8a9fb60e37f782a7dd9141ab2bb8ecacaeaa129178"
            }
        }
    ],
    "related": null,
    "maxidx": 1
}
```
##### Bob's State (Alice is in the "Requests" list):
```json
{
    "dapid": "c78a05c06876a61a3942c2e5618ceec0a51e301b2b708f908165a2c00ca32cb8",
    "buid": "c5a653f70aac16e8a99baa8a9fb60e37f782a7dd9141ab2bb8ecacaeaa129178",
    "objects": [
        {
            "act": 1,
            "objtype": "user",
            "rev": 0,
            "idx": 0,
            "aboutme": "I'm bob"
        }
    ],
    "related": [
        {
            "meta": {
                "sig": "1ae00a11b6ea301fa0d7e42b8c02af5bb9f5533aee824d16abe4950071031d14",
                "buid": "9ac9f8f7082f9d78975e9e6c186c7f358ee7c6855cedea4b06b10df5d34fdabc",
                "uname": "alice"
            },
            "act": 1,
            "objtype": "contact",
            "rev": 0,
            "idx": 1,
            "user": {
                "type": 0,
                "userId": "c5a653f70aac16e8a99baa8a9fb60e37f782a7dd9141ab2bb8ecacaeaa129178"
            }
        }
    ],
    "maxidx": 0
}
```
#### Bob accepted Alice's contact request:
##### Alice`s State (Bob is in the "Contacts" list):
```json
{
    "dapid": "c78a05c06876a61a3942c2e5618ceec0a51e301b2b708f908165a2c00ca32cb8",
    "buid": "9ac9f8f7082f9d78975e9e6c186c7f358ee7c6855cedea4b06b10df5d34fdabc",
    "objects": [
        {
            "act": 1,
            "objtype": "user",
            "rev": 0,
            "idx": 0,
            "aboutme": "I'm alice"
        },
        {
            "meta": {
                "sig": "1ae00a11b6ea301fa0d7e42b8c02af5bb9f5533aee824d16abe4950071031d14"
            },
            "act": 1,
            "objtype": "contact",
            "rev": 0,
            "idx": 1,
            "user": {
                "type": 0,
                "userId": "c5a653f70aac16e8a99baa8a9fb60e37f782a7dd9141ab2bb8ecacaeaa129178"
            }
        }
    ],
    "related": [
        {
            "meta": {
                "sig": "d803ca496eabe6546da01a0262ab3fdc60143ea263925640f6b360c924395cd6",
                "buid": "c5a653f70aac16e8a99baa8a9fb60e37f782a7dd9141ab2bb8ecacaeaa129178",
                "uname": "bob"
            },
            "act": 1,
            "objtype": "contact",
            "rev": 0,
            "idx": 1,
            "user": {
                "type": 0,
                "userId": "9ac9f8f7082f9d78975e9e6c186c7f358ee7c6855cedea4b06b10df5d34fdabc"
            }
        }
    ],
    "maxidx": 1
}
```
##### Bob's State (Alice is in the "Contacts" list):
```json
{
    "dapid": "c78a05c06876a61a3942c2e5618ceec0a51e301b2b708f908165a2c00ca32cb8",
    "buid": "c5a653f70aac16e8a99baa8a9fb60e37f782a7dd9141ab2bb8ecacaeaa129178",
    "objects": [
        {
            "act": 1,
            "objtype": "user",
            "rev": 0,
            "idx": 0,
            "aboutme": "I'm bob"
        },
        {
            "meta": {
                "sig": "d803ca496eabe6546da01a0262ab3fdc60143ea263925640f6b360c924395cd6"
            },
            "act": 1,
            "objtype": "contact",
            "rev": 0,
            "idx": 1,
            "user": {
                "type": 0,
                "userId": "9ac9f8f7082f9d78975e9e6c186c7f358ee7c6855cedea4b06b10df5d34fdabc"
            }
        }
    ],
    "related": [
        {
            "meta": {
                "sig": "1ae00a11b6ea301fa0d7e42b8c02af5bb9f5533aee824d16abe4950071031d14",
                "buid": "9ac9f8f7082f9d78975e9e6c186c7f358ee7c6855cedea4b06b10df5d34fdabc",
                "uname": "alice"
            },
            "act": 1,
            "objtype": "contact",
            "rev": 0,
            "idx": 1,
            "user": {
                "type": 0,
                "userId": "c5a653f70aac16e8a99baa8a9fb60e37f782a7dd9141ab2bb8ecacaeaa129178"
            }
        }
    ],
    "maxidx": 1
}
```
## Screenshots

| <img width="300" src="https://user-images.githubusercontent.com/564039/44699894-056bfe00-aa4c-11e8-8dfd-b7e252f0b660.png"/> | <img width="300" src="https://user-images.githubusercontent.com/564039/44699895-056bfe00-aa4c-11e8-9936-82afcc13a419.png"/> |
| ------------- | ------------- |
| Login  | User search |
| <img width="300" src="https://user-images.githubusercontent.com/564039/44699896-056bfe00-aa4c-11e8-8109-a2e7c3655a17.png"/> | <img width="300" src="https://user-images.githubusercontent.com/564039/44699897-056bfe00-aa4c-11e8-9a71-73294d6e82c1.png"/> |
| Added contacts, waiting for approval | Contact request  |
| <img width="300" src="https://user-images.githubusercontent.com/564039/44699898-056bfe00-aa4c-11e8-92c3-3736ec240f8c.png"/> |
| Approved contact | |
