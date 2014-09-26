var Traffic = {

	update: function() {
		if (android.traffic()) {
			if (this.traffic)
				return;
			this.traffic = L.tileLayer("http://jn{s}maps.mail.ru/tiles/newjams/{z}/{y}/{x}.png", {
				subdomains: '0123456',
				format: 'image/png',
				transparent: true
			});
			map.addLayer(this.traffic);
			return;
		}
		if (this.traffic == null)
			return;
		map.removeLayer(this.traffic);
		this.traffic = null;
	}

}

function showTraffic() {
	Traffic.update();
}