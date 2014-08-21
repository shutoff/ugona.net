android = {

	getLocation: function() {
		return "60.011,30.009,500"
	},

	getData: function() {
		return "60.01;30.01;103;;Test point|60.0003;30.0001;;;Point 2";
	},

//	getZone: function() {
//		return "60.01,30.01,60.02,30.02"
//	},

	init: function() {
		return [
			"showTraffic()",
			"myLocation()",
			"showPoints()",
			"showZone()"
		].join("\n");
	},

	traffic: function() {
		return true;
	}
}