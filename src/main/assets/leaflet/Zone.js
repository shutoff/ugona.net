
function showZone() {

	if (android.getZone == null)
		return;

	var bounds = getBounds();
	var p1 = bounds[0];
	var p2 = bounds[1];
	var rectangle = L.rectangle([
		[p1[0], p1[1]],
		[p2[0], p2[1]]
	], {
		color: '#FF0000',
		ppacity: 0.8,
		weight: 2,
		fillColor: '#FF0000',
		fillOpacity: 0.35
	});
	map.addLayer(rectangle);
	rectangle.editing.options.resizeIcon = new L.DivIcon({
		iconSize: new L.Point(16, 16),
		className: 'leaflet-div-icon leaflet-editing-icon leaflet-edit-resize'
	});
	rectangle.editing.options.moveIcon = new L.DivIcon({
		iconSize: new L.Point(20, 20),
		className: 'leaflet-div-icon leaflet-editing-icon leaflet-edit-resize'
	});
	rectangle.editing.enable();

	rectangle.on('edit', function() {
		var z = rectangle.getBounds();
		android.setZone(z.getNorth() + ',' + z.getWest() + ',' + z.getSouth() + ',' + z.getEast());
	});
}