// geofire-helper.js
const geofire = require('geofire-common');
const admin = require('firebase-admin');

const db = admin.database();

async function getDriversWithinArea(center, radiusInKm) {
    const radiusInM = radiusInKm * 1000; // Radius in meters
    const bounds = geofire.geohashQueryBounds(center, radiusInM);

    const promises = bounds.map((b) => {
        return db.ref("drivers")
            .orderByChild("geoHash")
            .once('value');
    });

    const snapshots = await Promise.all(promises);
    const matchingDocs = {};

    snapshots.forEach((snap) => {
        snap.forEach((doc) => {
            const location = doc.val().location.split(",");
            const distanceInKm = geofire.distanceBetween([parseFloat(location[0]), parseFloat(location[1])],center);
            
            const distanceInM = distanceInKm * 1000;
            if (distanceInM <= radiusInM) {
                   matchingDocs[doc.key] = doc.val();
            }
        });
    });
    return matchingDocs;
}

module.exports = { getDriversWithinArea };
