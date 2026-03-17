package com.expensio.utils

class OfflineQueuedException(message: String = "Saved offline — will sync when connected") : Exception(message)
