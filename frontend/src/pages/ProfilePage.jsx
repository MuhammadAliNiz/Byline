import { useEffect, useState } from 'react'
import Alert from '../components/Alert'
import LoadingSpinner from '../components/LoadingSpinner'
import { useAuth } from '../context/authContext'

function ProfileItem({ label, value }) {
  return (
    <div>
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <p className="text-sm font-medium">{value || '—'}</p>
    </div>
  )
}

export default function ProfilePage() {
  const { getMe } = useAuth()
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let mounted = true

    const loadProfile = async () => {
      try {
        const data = await getMe()
        if (!mounted) return
        setProfile(data)
      } catch (err) {
        if (!mounted) return
        setError(err.message || 'Could not load profile.')
      } finally {
        if (mounted) {
          setLoading(false)
        }
      }
    }

    loadProfile()

    return () => {
      mounted = false
    }
  }, [getMe])

  const fullName = `${profile?.firstName || ''} ${profile?.lastName || ''}`.trim() || 'User'

  if (loading) {
    return <LoadingSpinner text="Loading profile..." />
  }

  if (error) {
    return <Alert type="error">{error}</Alert>
  }

  return (
    <div className="space-y-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex items-center gap-4">
        <img
          alt={`${profile?.username || 'User'} avatar`}
          className="h-16 w-16 rounded-full border border-slate-200 object-cover"
          onError={(event) => {
            event.currentTarget.src = 'https://ui-avatars.com/api/?name=User'
          }}
          src={profile?.avatarUrl || 'https://ui-avatars.com/api/?name=User'}
        />
        <div>
          <h1 className="text-2xl font-bold">{fullName}</h1>
          <p className="text-sm text-slate-600">@{profile?.username}</p>
        </div>
      </div>

      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        <ProfileItem label="Email" value={profile?.email} />
        <ProfileItem label="Role" value={profile?.role} />
        <ProfileItem label="Email Verified" value={profile?.emailVerified ? 'Yes' : 'No'} />
        <ProfileItem label="Followers" value={profile?.followersCount} />
        <ProfileItem label="Following" value={profile?.followingCount} />
        <ProfileItem label="Articles" value={profile?.articlesCount} />
        <ProfileItem label="Website" value={profile?.websiteUrl} />
        <ProfileItem label="Twitter" value={profile?.twitterHandle} />
        <ProfileItem label="LinkedIn" value={profile?.linkedinUrl} />
        <ProfileItem label="GitHub" value={profile?.githubUrl} />
        <ProfileItem label="Bio" value={profile?.bio} />
      </div>
    </div>
  )
}
