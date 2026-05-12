import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../store/authStore';
import toast from 'react-hot-toast';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [form, setForm] = useState({
    companyName: '', slug: '', email: '', password: '',
    firstName: '', lastName: '',
  });
  const [loading, setLoading] = useState(false);
  const set = (field) => (e) => setForm((p) => ({ ...p, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authApi.register(form);
      login(res.data.token, {
        userId: res.data.userId,
        tenantId: res.data.tenantId,
        email: res.data.email,
        role: res.data.role,
        companyName: res.data.companyName,
      });
      toast.success('Account created!');
      navigate('/dashboard');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="bg-white rounded-xl shadow-lg p-8 w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-blue-600">☁️ CloudFlow</h1>
          <p className="text-gray-500 mt-2">Create your company account</p>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          {[
            { field: 'companyName', label: 'Company Name', placeholder: 'Acme Corp' },
            { field: 'slug', label: 'Company Slug', placeholder: 'acmecorp' },
            { field: 'email', label: 'Email', type: 'email', placeholder: 'admin@acme.com' },
            { field: 'password', label: 'Password', type: 'password', placeholder: '••••••••' },
            { field: 'firstName', label: 'First Name', placeholder: 'John' },
            { field: 'lastName', label: 'Last Name', placeholder: 'Doe' },
          ].map(({ field, label, type = 'text', placeholder }) => (
            <div key={field}>
              <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
              <input type={type} value={form[field]} onChange={set(field)}
                required={['companyName','slug','email','password'].includes(field)}
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder={placeholder} />
            </div>
          ))}
          <button type="submit" disabled={loading}
            className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium">
            {loading ? 'Creating account...' : 'Create Account'}
          </button>
        </form>
        <p className="text-center text-sm text-gray-500 mt-4">
          Already have an account?{' '}
          <Link to="/login" className="text-blue-600 hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  );
}