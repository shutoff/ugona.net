var Points = {
	update: function() {
		getData();
		if (this.markers == null) {
			this.markers = [];
			this.info = [];
		}
		if (this.markers.length != this.data.length) {
			for (var i = 0; i < this.markers.length; i++) {
				map.removeLayer(this.markers[i]);
			}
			this.markers = [];
			this.info = [];
		}
		for (var i = 0; i < this.data.length; i++) {
			var p = this.data[i];
			var lat = parseFloat(p[0]);
			var lng = parseFloat(p[1]);
			if (this.markers[i] == null) {
				var icon;
				var course = 0;
				if (p[2] != '') {
					if (i == 0) {
						if (this.icon_active_arrow == null) {
							this.icon_active_arrow = L.icon({
								iconUrl: '../img/cur_arrow.png',
								iconSize: [24, 24],
								iconAnchor: [12, 12]
							});
						}
						icon = this.icon_active_arrow;
					} else {
						if (this.icon_arrow == null) {
							this.icon_arrow = L.icon({
								iconUrl: '../img/arrow.png',
								iconSize: [24, 24],
								iconAnchor: [12, 12]
							});
						}
						icon = this.icon_arrow;
					}
					course = parseFloat(p[2]);
				} else {
					if (i == 0) {
						if (this.icon_active == null) {
							this.icon_active = L.icon({
								iconUrl: '../img/cur_marker.png',
								iconSize: [24, 24],
								iconAnchor: [12, 12]
							});
						}
						icon = this.icon_active;
					} else {
						if (this.icon_inactive == null) {
							this.icon_inactive = L.icon({
								iconUrl: '../img/marker.png',
								iconSize: [24, 24],
								iconAnchor: [12, 12]
							});
						}
						icon = this.icon_inactive;
					}
				}
				this.markers[i] = new L.Marker([lat, lng], {
					icon: icon,
					iconAngle: course
				});
				map.addLayer(this.markers[i]);
			} else {
				this.markers[i].setLatLng([lat, lng]);
			}
			this.setInfo(i);
		}
	},

	setInfo: function(i) {
		var text = this.text(i);
		if (text != "") {
			var p = this.data[i];
			var lat = parseFloat(p[0]);
			var lng = parseFloat(p[1]);
			var info = L.popup()
				.setLatLng([lat, lng])
				.setContent(text);
			this.markers[i].on('click', function() {
				info.addTo(map);
			});
		}
	},

	text: function(i) {
		var data = this.data[i];
		var res = data[4];
		if (data[6])
			res += '<br/>' + data[6];
		return res;
	}
}

function getData() {
	var data = android.getData();
	var points = (data + "").split('|');
	Points.data = [];
	for (var i = 0; i < points.length; i++) {
		Points.data.push(points[i].split(';'));
	}
}

function getBounds() {
	if (android.getZone) {
		var bounds = (android.getZone() + '').split(',');
		return [
			[parseFloat(bounds[0]), parseFloat(bounds[1])],
			[parseFloat(bounds[2]), parseFloat(bounds[3])]
		];
	}
	if (android.getData) {
		getData();
		var bounds = [
			[parseFloat(Points.data[0][0]), parseFloat(Points.data[0][1])]
		];
		if (Points.data[0][5]) {
			var points = Points.data[5].split('|');
			var min_lat = 180;
			var max_lat = -180;
			var min_lon = 180;
			var max_lon = -180;
			for (var i in points) {
				var p = points[i].split(',');
				bounds.push([parseFloat(p[0]), parseFloat(p[1])]);
			}
		}
		return bounds;
	}
}

function showPoints() {
	Points.update();
}