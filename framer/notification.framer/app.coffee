layer = new BackgroundLayer 
	image: "images/background2.png"
	
#Scrollable notification box

notification = new Layer
	width: 320
	height: 220
	x: 0
	y: 290
	backgroundColor: "#76c2d2"
	
notification.draggable.enabled = true
notification.draggable.horizontal = false

notification.html = "<div> An audio recording is nearby! <br> <br>Title: A family picnic </div> <br>"

notification.style["text-align"] = "center"
notification.style["padding-top"] = "20px"
notification.style["color"] = "#141148"

notification.draggable.constraints = {
    x: 0
    y: 90
    height: 440
    width: 320
}

notification.states.add
	fade : { opacity : 0}

notification_button = new Layer
	width : 100
	height : 35
	x : 110
	y : 160
	borderRadius: 6
	backgroundColor : "#e2e2e2"
	
notification_button.superLayer = notification
notification_button.html = "Listen!"

notification_button.on Events.Click, ->
	notification.states.switchInstant("fade")
	confirm_box.visible = true
	layer.opacity = .2
	
#Confirm textbox
confirm_box = new Layer
	width: 300
	height: 300
	backgroundColor: null
confirm_box.center()
confirm_box.visible = false
	
confirm = new Layer
	width: 190
	height: 190
	borderRadius: 100
	superLayer: confirm_box
confirm.centerX()
confirm.image = "images/okay.png"

confirm.on Events.click, ->
	confirm_box.visible = false
	layer.opacity = 1

confirm_text = new Layer
	width: 250
	height: 100
	y : 200
	backgroundColor: null
	superLayer: confirm_box
confirm_text.centerX()
confirm_text.html = "<p>Recording sent to your phone!</p>"

confirm_text.on Events.click, ->
	confirm_box.visible = false
	layer.opacity = 1




	