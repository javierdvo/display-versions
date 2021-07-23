package com.javierdvo.link.classes.devices

data class Display(
        var id: String,
        var macAddress: String,
        var name: String,
        var displayNum: Int,
        var allowedUserIds: List<Int>
) {
        constructor(macAddress: String) : this(
                "",macAddress,"",0, ArrayList()
        )
        constructor() : this(
                "","","",0, ArrayList()
        )
}
