const express = require('express');
const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getDatabase } = require('firebase-admin/database');
const { getAuth } = require('firebase-admin/auth');
const { database } = require('firebase-admin');
const { getMessaging } = require('firebase-admin/messaging');
const HereSdk = require('./hereSdk');

const port = 3000;
const app = express();
app.use(express.json());



initializeApp({
  credential: applicationDefault(),
  databaseURL: "https://webapp-303e9-default-rtdb.asia-southeast1.firebasedatabase.app"
});

const { getDriversWithinArea } = require('./geofireHelper');
const e = require('express');

var db = getDatabase();
var userRef = db.ref("users");
var driverRef = db.ref("drivers");
var tripsRef = db.ref("trips");

async function verifyToken(idToken) {
  try {
    const decodedToken = await getAuth().verifyIdToken(idToken);
    const uid = decodedToken.uid;
    return uid;
  } catch (error) {
    console.error('Error verifying ID token:', error);
    return null;
  }
}

app.post('/bookRide', async (req, res) => {
  const idToken = req.headers.token;
  const uid = await verifyToken(idToken);
  console.log("create ride: " + uid)
  const jsonData = req.body;

  const tripRef = tripsRef.child(uid);
  /*  check if there is any ongoing ride  */
  tripRef.orderByChild("status").equalTo(0).get().then((snapshot) => {
    if (snapshot.exists()) {//ride found
      res.statusCode = 400;
      res.json({ error: "There is an ongoing ride", "tripId": snapshot.val() });
      return;
    }

    const location = jsonData.location.split(',');

    //no ongoing rides. create one
    HereSdk.getRoute(jsonData.location, jsonData.destination).then(async (route) => {
      if (route.length < 3000) {
        throw Error('Ride destance must be greater then 3.0 km')
      }
      let fare = parseInt((route.length / 1000) * 30);
      fare += parseInt((jsonData.passengers - 1) * 30 / 2);//extra 50% fare per passenger
      jsonData["date"] = database.ServerValue.TIMESTAMP;
      jsonData["status"] = 0;//0 for ongoing trip
      jsonData['distance'] = route.length;
      jsonData['eta'] = route.duration;
      jsonData['fare'] = fare;
      const tripID = tripRef.push().key;
      //notify drivers of new ride
      await getDriversWithinArea([parseFloat(location[0]), parseFloat(location[1])], 10).then((drivers) => {
        if (drivers.length <= 0) {
          throw Error('Failed to fetch driveres near you');
        }
        for (const [key, value] of Object.entries(drivers)) {
         // if (key === uid) continue;//passenger cant be a driver
          getMessaging().send({
            token: value.msgToken,
            data: {
              type: "passengerFound",
              title: "A new Ride found",
              uid: uid,
              tripId: tripID
            }
          });
        }
        //update db
        tripRef.child(tripID).set(jsonData);
        res.statusCode = 200;
        res.json({ message: "successfull", "tripId": tripID });

      }).catch((error) => {
        throw error;
      });

    }).catch((error) => {
      console.error(error);
      res.statusCode = 401;
      res.json({ error: "Failed to retreve route details" });
      return;
    });
  });
});

app.post("/acceptRide", async (req, res) => {
  const jsonData = req.body;
  const idToken = req.headers.token;
  const uid = await verifyToken(idToken);
  const tripId = jsonData.tripId;
  const passengerId = jsonData.passengerId;
  console.log("acceptRidsde: " + passengerId);
  tripsRef.child(passengerId + "/" + tripId).get().then((dataSnapshot) => {
    if (dataSnapshot.child("status").val() != 0) {
      res.status = 400;
      res.json({ error: "Ride is already accepted by another driver" });
      return;
    }
    const rideInstance = { passengerUid: passengerId, status: 1 };
    driverRef.child(uid + "/trips/" + tripId).set(rideInstance).then(() => {
      const update = { driverUid: uid, status: 1 };
      tripsRef.child(passengerId + "/" + tripId).update(update);
      /*  getMessaging().send({
          token: msgToken,
          data: {
            type: "passengerFound",
            title: "A new Ride found",
            uid: uid,
            tripId: tripId
          }
        });*/
      res.status = 200;
      res.json({ message: "successfull" });
    }).catch((error) => { throw error; });
  }).catch((error) => {
    res.status = 400;
    res.json({ error: "Failed to get ride details" });
  });
});

app.post('cancelRide', async (req, res) => {
  const jsonData = req.body;
  const idToken = req.headers.token;
  const uid = await verifyToken(idToken);
  const tripId = jsonData.tripID;
  tripsRef.child(uid + "/" + tripId+"/status").get().then((dataSnapshot) => {
    if (dataSnapshot.val() > 0){
      tripsRef.child(uid + "/" + tripId+"/status").set(-2);
    res.status = 200;
    res.end();
    }else{
    res.status = 400;
    res.json({ error: "Ride is already finished" });
    }
  })
});
app.post("/askConfermation", async (req, res) => {
  console.log("askConfermation");
  const jsonData = req.body;
  const idToken = req.headers.token;
  const uid = await verifyToken(idToken);
  const passengerId = jsonData.pasengerId;
  let tripRef = tripsRef.child(passengerId + "/" + tripId);
  tripRef.get().then((dataSnapshot) => {
    if (dataSnapshot.child("driverUid").val() === uid
      && dataSnapshot.child("status").val() === 1) {
      tripRef.child("status").set(2);
      res.statusCode = 200;
      res.end(JSON.stringify({ message: "successfull" }));
    } else if (dataSnapshot.child("driverUid").val() === uid
      && dataSnapshot.child("status").val() === 3) {
      tripRef.child("status").set(-1);
    } else {
      throw new Error('Server error');
    }
  }).catch((error) => {
    res.statusCode = 400;
    res.end(JSON.stringify({ error: error }));
  });

});
app.post("/rideFinished", async (req, res) => {
  const jsonData = req.body;
  const idToken = req.headers.token;
  const uid = await verifyToken(idToken);
  //tripsRef.child(uid + "/" + jsonData.tripId+"/status").set(-1);
    res.statusCode = 200;
    res.end();
});

app.post("/validatedriver", async (req, res) => {
  const jsonData = req.body;
  const idToken = req.headers.token;
  const uid = await verifyToken(idToken);
  await getAuth().getUser(uid).then((user) => {
    jsonData["uname"] = user.displayName;
  }).catch((error) => {
    console.error(error);
    res.statusCode = 400;
    res.json({ error: 'An error occured while registering the user', message: 'Validation Failed' });
    return;
  });
  //these valuse must be obtained after verifing vehicle owner,vehicle name etc
  jsonData["taxiType"] = "CAR";
  jsonData["vehicleName"] = "Innova";
  if (uid != null) {
    console.log("register driver uid: " + uid);
    //verify the user's identity here 
    driverRef.child(uid).set(jsonData).then(() => {
      //if verified, update the database 
      userRef.child(uid).update({ userType: "PENDING_PAYMENT_PROFILE" });
      res.statusCode = 200;
      res.end(JSON.stringify({ message: "successfull" }));
    })
      .catch((error) => {
        //else return error
        res.statusCode = 400;
        res.json({ error: 'An error occured while registering the user', message: 'Validation Failed' });
      });
  } else {
    res.end({ error: "error" });
  }

  console.log("validateuser" + res.statusCode + uid);
});

app.post("/updatepaymentdetails", async (req, res) => {
  const idToken = req.headers.token;
  const jsonData = req.body;
  const uid = await verifyToken(idToken);
  if (uid != null) {
    console.log("uid: " + uid);
    //verify the user's identity here 
    driverRef.child(uid + "/payment").set(jsonData).then(() => {
      //if verified, update the database 
      userRef.child(uid).update({ userType: "DRIVER" });
      res.statusCode = 200;
      res.json({ message: "successfull" });
    })
      .catch((error) => {
        //else return error
        res.statusCode = 400;
        res.json({ error: 'An error occured while setting payment details' });
      });
  } else {
    res.json({ error: "error" });
  }
});



app.listen(port, () => {
  console.log(`Server running at http://127.0.0.1:${port}/`);
});