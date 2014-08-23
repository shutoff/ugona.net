var Points = {
	update: function() {
		getData();
		if (this.markers == null) {
			this.markers = [];
			this.zones = [];
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
			var icon;
			var course = 0;
			if (p[4]) {
				if (this.markers[i] != null) {
					map.removeLayer(this.markers[i]);
					this.markers[i] = null;
				}
				var path = [];
				var points = p[4].split('_');
				for (var n = 0; n < points.length; n++) {
					var point = points[n].split(',');
					path.push(new L.LatLng(point[0], point[1]));
				}
				path.pop();
				var color = '#FF0000';
				var opacity = 0.1;
				if (i > 0) {
					color = '#0000FF';
					opacity = 0.05;
				}
				var zone = L.polygon(path, {
					stroke: true,
					color: color,
					weight: 2,
					opacity: opacity * 5,
					fill: true,
					fillColor: color,
					fillOpacity: opacity
				}).addTo(map);
				this.markers[i] = zone;
			} else {
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
				try {
					this.markers[i].setLatLng([lat, lng]);
					this.markers[i].setIcon(icon);
					this.markers[i].setIconAngle(course);
				} catch (err) {
					if (this.markers[i])
						map.removeLayer(this.markers[i]);
					this.markers[i] = new L.Marker([lat, lng], {
						icon: icon,
						iconAngle: course
					});
					map.addLayer(this.markers[i]);
				}
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
			this.markers[i].on('click', function() {
				showPopup(lat, lng, text)
			});
		}
	},

	showPopup: function() {
		var text = this.text(0);
		if (text != "") {
			var p = this.data[0];
			var lat = parseFloat(p[0]);
			var lng = parseFloat(p[1]);
			showPopup(lat, lng, text);
		}
	},

	text: function(i) {
		var data = this.data[i];
		var lines = data[3].split('\n');
		var words = lines[0].split(' ');
		lines[0] = '<b>' + words.shift() + '</b> ' + words.join(' ');
		var res = lines.join('<br>');
		if (data[5])
			res += '<br/>' + data[5];
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
	if (android.getTracks)
		return Tracks.getBounds();
	if (android.getData) {
		getData();
		var bounds = [
			[parseFloat(Points.data[0][0]), parseFloat(Points.data[0][1])]
		];
		if (Points.data[0][4]) {
			var points = Points.data[0][4].split('_');
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
	console.log('fail')
}

function showPoints() {
	Points.update();
}