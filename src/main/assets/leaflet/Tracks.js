var colors =
	[
		[5, '#800000'],
		[10, '#C00000'],
		[20, '#C04000'],
		[30, '#C08000'],
		[40, '#A08000'],
		[50, '#408000'],
		[60, '#00A000'],
		[90, '#00A020'],
		[0, '#00A080']
	];

var Tracks = {
	update: function() {
		for (var i in this.markers) {
			(function(p) {
				var lat = parseFloat(p[0]);
				var lon = parseFloat(p[1]);
				var mark = L.marker([lat, lon]);
				map.addLayer(mark);
				mark.on('click', function() {
					L.popup()
						.setLatLng([lat, lon])
						.setContent(p[2])
						.openOn(map)
				});
			})(this.markers[i]);
		}
		if (this.tracks) {
			for (var i in this.tracks) {
				map.removeLayer(this.tracks[i]);
			}
		}
		this.tracks = [];
		var traffic = android.traffic();
		for (var i in this.points) {
			var p = this.points[i];
			var line = L.polyline(p.points, {
					color: traffic ? colors[p.index][1] : '#000080',
					weight: 7,
					opacity: 1
				})
				.addTo(map);
			this.tracks.push(line);
			line.on('click', showPointInfo);
		}
	},

	init: function() {
		var track_data = android.getTracks();
		this.parts = (track_data + "").split('|');

		this.points = [];
		this.markers = [];

		this.min_lat = 180;
		this.min_lng = 180;
		this.max_lat = -180;
		this.max_lng = -180;

		var last_mark = false;
		for (var i in this.parts) {
			var p = this.parts[i].split(',');
			var lat = parseFloat(p[0]);
			var lon = parseFloat(p[1]);
			if (lat > this.max_lat)
				this.max_lat = lat;
			if (lat < this.min_lat)
				this.min_lat = lat;
			if (lon > this.max_lng)
				this.max_lng = lon;
			if (lon < this.min_lng)
				this.min_lng = lon;
			if (p.length == 4) {
				var speed = parseFloat(p[2]);
				for (var index = 0; index < colors.length - 1; index++) {
					if (colors[index][0] >= speed)
						break;
				}
				var point = [lat, lon];
				if (last_mark) {
					last_mark = false;
				} else {
					if (this.points.length) {
						var last = this.points[this.points.length - 1];
						last.points.push(point);
						if (last.index == index)
							index = -1;
					}
				}
				if (index >= 0) {
					var last = {
						index: index,
						points: [point]
					}
					this.points.push(last);
				}
			} else if (p.length == 3) {
				this.markers.push(p);
				last_mark = true;
			} else {
				last_mark = true;
			}
		}

	},

	getBounds: function() {
		if (!this.points)
			this.init();
		return [
			[this.min_lat, this.min_lng],
			[this.max_lat, this.max_lng]
		];
	}

}

function showPointInfo(event) {
	var delta = 1000;
	var best_index = null;
	for (var i in Tracks.parts) {
		var p = Tracks.parts[i].split(',');
		if (p.length != 4)
			continue;
		var lat = parseFloat(p[0]);
		var lon = parseFloat(p[1]);
		var d = Math.abs(lat - event.latlng.lat) + Math.abs(lon - event.latlng.lng);
		if (d < delta) {
			best_index = i;
			delta = d;
		}
	}
	if (best_index == null)
		return;
	var p = Tracks.parts[best_index].split(',');
	var d = new Date(parseInt(p[3]));
	var lat = parseFloat(p[0]);
	var lon = parseFloat(p[1]);
	if (Tracks.point_info == null)
		Tracks.point_info = L.popup();
	Tracks.point_info
		.setLatLng([lat, lon])
		.setContent(d.toLocaleTimeString() + '<br/>' + p[2] + ' ' + android.kmh())
		.addTo(map);
}


function showTracks() {
	return Tracks.update();
}