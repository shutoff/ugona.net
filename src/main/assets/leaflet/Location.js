var Location = {

	update: function() {
		if (map == null)
			return;
		var location = android.getLocation().split(',');
		if (location.length < 3)
			return;
		var lat = location[0];
		var lng = location[1];
		var radius = parseFloat(location[2]);
		if (this.icon == null) {
			this.icon = L.icon({
				iconUrl: '../img/person.png',
				iconSize: [48, 48],
				iconAnchor: [24, 48]
			});
		}
		if (this.location == null) {
			this.location = new L.Circle([lat, lng], radius, {
				stroke: true,
				color: '#0000FF',
				weight: 2,
				opacity: 0.1,
				fill: true,
				fillColor: '#0000FF',
				fillOpacity: 0.05
			});
			map.addLayer(this.location);
			this.marker = new L.Marker([lat, lng], {
				icon: this.icon
			});
			map.addLayer(this.marker);
		} else {
			this.location.setLatLng([lat, lng]);
			this.location.setRadius(radius);
			this.marker.setLatLng([lat, lng]);
			this.marker.setIcon(this.icon);
		}
	}

}

function myLocation() {
	Location.update();
}

function getRect(bounds) {
	var min_lat = 180;
	var min_lng = 180;
	var max_lat = -180;
	var max_lng = -180;
	for (var i = 0; i < bounds.length; i++) {
		var point = bounds[i];
		var lat = point[0];
		var lng = point[1];
		if (lat < min_lat)
			min_lat = lat;
		if (lat > max_lat)
			max_lat = lat;
		if (lng < min_lng)
			min_lng = lng;
		if (lng > max_lng)
			max_lng = lng;
	}
	return [
		[min_lat, min_lng],
		[max_lat, max_lng]
	]
}

function fitBounds(bounds, k) {
	var rect = getRect(bounds);
	var min_point = rect[0];
	var max_point = rect[1];
	var min_lat = min_point[0];
	var min_lng = min_point[1];
	var max_lat = max_point[0];
	var max_lng = max_point[1];
	var d_lat = (max_lat - min_lat) * k;
	var d_lng = (max_lng - min_lng) * k;
	var zoom = map.getZoom();
	if (zoom < 12)
		zoom = 12;
	map.fitBounds([
		[min_lat - d_lat, min_lng - d_lng],
		[max_lat + d_lat, max_lng + d_lng]
	], {
		maxZoom: zoom,
		animate: true
	});
}


function setPosition() {
	if (map == null)
		return;

	var pos = android.getLocation().split(',');
	if (pos.length < 3)
		return;

	var bounds = getBounds();
	bounds.push([parseFloat(pos[0]), parseFloat(pos[1])]);

	fitBounds(bounds, 0.125);
}

function center() {
	if (map == null)
		return;
	fitBounds(getBounds(), 0.125);
	Points.showPopup();
}
