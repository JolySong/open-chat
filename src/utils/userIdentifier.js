import FingerprintJS from '@fingerprintjs/fingerprintjs'

const FIRST_NAMES = [
  'Alex', 'Blake', 'Charlie', 'Drew', 'Eden', 
  'Finn', 'Gray', 'Harper', 'Ivy', 'Jordan',
  'Kelly', 'Logan', 'Morgan', 'Noah', 'Parker',
  'Quinn', 'Riley', 'Sam', 'Taylor', 'Val'
];

const LAST_NAMES = [
  'Smith', 'Johnson', 'Brown', 'Davis', 'Wilson',
  'Moore', 'Taylor', 'Anderson', 'Thomas', 'White',
  'Harris', 'Martin', 'Thompson', 'Young', 'King',
  'Wright', 'Lee', 'Walker', 'Hall', 'Allen'
];

class UserIdentifier {
  static instance = null
  static fingerprint = null

  static async getInstance() {
    if (!this.instance) {
      this.instance = await FingerprintJS.load()
    }
    return this.instance
  }

  static async getFingerprint() {
    if (!this.fingerprint) {
      const fp = await this.getInstance()
      const result = await fp.get()
      this.fingerprint = result.visitorId
    }
    return this.fingerprint
  }

  static async generateUsername() {
    const fingerprint = await this.getFingerprint()
    // Use fingerprint to consistently select names
    const hash = Array.from(fingerprint).reduce((acc, char) => acc + char.charCodeAt(0), 0)
    const firstName = FIRST_NAMES[hash % FIRST_NAMES.length]
    const lastName = LAST_NAMES[(hash * 13) % LAST_NAMES.length]
    return `${firstName}${lastName}`
  }
}

export default UserIdentifier 