
var platform = new H.service.Platform({
  apikey: 'apiKey'
});
var defaultLayers = platform.createDefaultLayers();
var map;
var loct;
var destination;

map = new H.Map(document.getElementById('mapContainer'),
  defaultLayers.vector.normal.map, {
  center: { lat: 21.1290, lng: 82.794998 },
  zoom: 14,
  pixelRatio: window.devicePixelRatio || 1
});

window.addEventListener('resize', () => map.getViewPort().resize());

const mapEvents = new H.mapevents.MapEvents(map);
var behavior = new H.mapevents.Behavior(mapEvents);
var marker, driverMarker;
var polyline;
var navGroup;
var primaryRoute;
var routeGroup = new H.map.Group();

var ui = H.ui.UI.createDefault(map, defaultLayers);

map.addEventListener('tap', function (evt) {
  // Get the coordinates where the user clicked
  const coords = map.screenToGeo(
    evt.currentPointer.viewportX,
    evt.currentPointer.viewportY
  );

  //landmarkGeocode(platform);
  Android.locationPicked(coords.lat.toFixed(6), coords.lng.toFixed(6));
  marker = new H.map.Marker(coords);
  //calculateRouteFromAtoB(platform, `${coords.lat.toFixed(6)},${coords.lng.toFixed(6)}`);
});

Android.getOrigin();
//setCenter({ lat: 34.1649493, lng: 74.9128038 });
//drawNavIcon(34.1649493,74.9128038,34.148801,74.872639);

function addRouteShapeToMap(route, strokeColor, removeOldRoute) { // remove any objects from the map if any 
  if (map.getObjects().length > 0) {
    if (removeOldRoute) {
      if (routeGroup.getObjects().length > 0) map.removeObjects(routeGroup.getObjects());
      if (primaryRoute != null) map.removeObjects(primaryRoute.getObjects());
      routeGroup = null;
      primaryRoute = null;
    } else {//exclude primary route
      if (routeGroup.getObjects().length > 0) routeGroup.removeObjects(routeGroup.getObjects);
    }
  }

  objectsGroup = new H.map.Group();
  route.sections.forEach((section) => {
    // decode LineString from the flexible polyline
    let linestring = H.geo.LineString.fromFlexiblePolyline(section.polyline);

    // Create a polyline to display the route:
    polyline = new H.map.Polyline(linestring, {
      style: {
        lineWidth: 5,
        strokeColor: strokeColor
      }
    });
    objectsGroup.addObject(polyline);
  });
  if (primaryRoute == null) {
    primaryRoute = objectsGroup;
    map.addObject(primaryRoute);
  } else {
    routeGroup = objectsGroup;
    map.addObject(routeGroup);
  }
}

function calculateRouteFromAtoB(location, dest, strokeColor, removeOldRoute, drawMarkersAtEnds) {
  loct = location;
  destination = dest;
  var router = platform.getRoutingService(null, 8),
    routeRequestParams = {
      routingMode: 'fast',
      transportMode: 'car',
      origin: location,
      destination: dest,
      return: 'polyline'
    };
  router.calculateRoute(
    routeRequestParams,
    (result) => {
      var route = result.routes[0];
      addRouteShapeToMap(route, strokeColor, removeOldRoute);
      if (!drawMarkersAtEnds)
        return;
      let array = loct.split(",");
      map.addObject(new H.map.Marker({ lat: array[0], lng: array[1] }));
      array = destination.split(",");
      map.addObject(new H.map.Marker({ lat: array[0], lng: array[1] }));
    },
    onError
  );
  map
  let array = location.split(",");
  map.setCenter({ lat: array[0], lng: array[1] });
}


function onError(error) {
  alert('Can\'t reach the remote server');
}

function setCenter(coords) {
  map.setCenter(coords);
  map.setZoom(14);
  if (map.getObjects().length > 0) {
    map.removeObjects(map.getObjects());
  }
  marker = new H.map.Marker(coords);
  map.addObject(marker);
}


async function drawNavIcon(oldLat, oldLng, newLat, newLng) {
  if (navGroup)
    map.removeObjects(navGroup);
  const rotation = calculateBearing(oldLat, oldLng, newLat, newLng);
  map.getViewModel().setLookAtData({
    heading: rotation,
    zoom: 15
  });

  navMarker = new H.map.Marker({ lat: newLat, lng: newLng }, { icon: rotatedNavSvg(rotation) });
  circle = new H.map.Circle({ lat: newLat, lng: newLng }, 200);
  navGroup = new H.map.Group({
    volatility: true,
    objects: [circle, navMarker]
  });
  map.addObject(navGroup);
  map.setCenter({ lat: newLat, lng: newLng });
}

function calculateBearing(lat1, lng1, lat2, lng2) {
  // Convert degrees to radians
  var toRadians = function (degrees) {
    return degrees * Math.PI / 180;
  };

  // Convert radians to degrees
  var toDegrees = function (radians) {
    return radians * 180 / Math.PI;
  };

  var dLng = toRadians(lng2 - lng1); // Difference in longitude

  // Convert latitude values to radians
  lat1 = toRadians(lat1);
  lat2 = toRadians(lat2);

  // Calculate bearing using the formula
  var y = Math.sin(dLng) * Math.cos(lat2);
  var x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);
  var bearing = Math.atan2(y, x);

  // Convert bearing from radians to degrees and normalize it to a 0-360 range
  bearing = toDegrees(bearing);
  return (bearing + 360) % 360; // Ensure bearing is in the range 0-360
}


function rotatedNavSvg(rotation) {
  svgMarkup = `<svg xmlns="http://www.w3.org/2000/svg" transform=" rotate(${rotation})" height="50px"  viewBox="0 -960 960 960" width="50px" fill="#173660"><path d="M480-81.87q-82.82 0-155.41-31.38T198.3-198.43q-53.69-53.79-85.06-126.31Q81.87-397.26 81.87-480q0-82.82 31.38-155.41t85.18-126.29q53.79-53.69 126.31-85.06 72.52-31.37 155.26-31.37 82.82 0 155.41 31.38t126.29 85.18q53.69 53.79 85.06 126.31 31.37 72.52 31.37 155.26 0 82.82-31.38 155.41T761.57-198.3q-53.79 53.69-126.31 85.06Q562.74-81.87 480-81.87Zm0-86q130.54 0 221.34-90.79 90.79-90.8 90.79-221.34 0-130.54-90.79-221.34-90.8-90.79-221.34-90.79-130.54 0-221.34 90.79-90.79 90.8-90.79 221.34 0 130.54 90.79 221.34 90.8 90.79 221.34 90.79ZM480-480ZM340.74-287.15 470.8-345.3q4.6-2.24 9.39-2.24 4.79 0 9.27 2.24l129.8 58.15q13.94 6.22 25.13-4.48 11.2-10.7 5.22-24.87L501.15-676.59q-5.89-13.93-21.11-13.93-15.21 0-21.19 13.93L310.39-316.5q-5.98 14.17 5.22 24.87 11.19 10.7 25.13 4.48Z"/></svg>`;
  return new H.map.Icon(svgMarkup, { anchor: { x: 25, y: 25 } });
}