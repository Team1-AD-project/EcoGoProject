import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  MessageSquare,
  Settings,
  Play,
  Pause,
  Edit,
  Trash2,
  Plus,
  RefreshCw,
  Server,
  Activity,
  Users,
  MessageCircle,
  CheckCircle,
  XCircle,
  Zap,
  BarChart3,
  TestTube,
  Save,
  X
} from 'lucide-react';

interface AIModel {
  id: string;
  name: string;
  displayName: string;
  description: string;
  status: 'active' | 'inactive';
  parameters: {
    temperature: number;
    maxTokens: number;
    topP: number;
  };
  stats: {
    totalRequests: number;
    activeUsers: number;
    avgResponseTime: number;
  };
  lastUsed: string;
}

interface APIConfig {
  endpoint: string;
  port: string;
  status: 'connected' | 'disconnected';
  version: string;
}

export function ChatManagement() {
  const [apiConfig, setApiConfig] = useState<APIConfig>({
    endpoint: 'http://localhost',
    port: '11434',
    status: 'connected',
    version: 'Ollama v0.1.0'
  });

  const [models, setModels] = useState<AIModel[]>([
    {
      id: '1',
      name: 'llama2',
      displayName: 'Llama 2',
      description: 'Meta\'s Llama 2 model for general conversation',
      status: 'active',
      parameters: {
        temperature: 0.7,
        maxTokens: 2048,
        topP: 0.9
      },
      stats: {
        totalRequests: 12450,
        activeUsers: 234,
        avgResponseTime: 1.2
      },
      lastUsed: '5 mins ago'
    },
    {
      id: '2',
      name: 'mistral',
      displayName: 'Mistral 7B',
      description: 'Efficient and powerful 7B parameter model',
      status: 'active',
      parameters: {
        temperature: 0.8,
        maxTokens: 4096,
        topP: 0.95
      },
      stats: {
        totalRequests: 8920,
        activeUsers: 156,
        avgResponseTime: 0.9
      },
      lastUsed: '12 mins ago'
    },
    {
      id: '3',
      name: 'codellama',
      displayName: 'Code Llama',
      description: 'Specialized model for coding assistance',
      status: 'inactive',
      parameters: {
        temperature: 0.5,
        maxTokens: 8192,
        topP: 0.85
      },
      stats: {
        totalRequests: 3240,
        activeUsers: 45,
        avgResponseTime: 1.5
      },
      lastUsed: '2 hours ago'
    }
  ]);

  const [editingModel, setEditingModel] = useState<AIModel | null>(null);
  const [isAddingModel, setIsAddingModel] = useState(false);
  const [newModel, setNewModel] = useState<Partial<AIModel>>({
    name: '',
    displayName: '',
    description: '',
    status: 'active',
    parameters: {
      temperature: 0.7,
      maxTokens: 2048,
      topP: 0.9
    }
  });
  const [isEditingAPI, setIsEditingAPI] = useState(false);
  const [testingModel, setTestingModel] = useState<string | null>(null);
  const [testPrompt, setTestPrompt] = useState('');
  const [testResponse, setTestResponse] = useState('');

  const totalRequests = models.reduce((sum, model) => sum + model.stats.totalRequests, 0);
  const activeModels = models.filter(m => m.status === 'active').length;
  const totalActiveUsers = models.reduce((sum, model) => sum + model.stats.activeUsers, 0);

  const toggleModelStatus = (modelId: string) => {
    setModels(models.map(model =>
      model.id === modelId
        ? { ...model, status: model.status === 'active' ? 'inactive' : 'active' }
        : model
    ));
  };

  const deleteModel = (modelId: string) => {
    if (confirm('Are you sure you want to delete this model?')) {
      setModels(models.filter(model => model.id !== modelId));
    }
  };

  const testConnection = () => {
    // Simulate connection test
    alert(`Testing connection to ${apiConfig.endpoint}:${apiConfig.port}...`);
  };

  const handleTestModel = (modelId: string) => {
    setTestingModel(modelId);
    setTestPrompt('');
    setTestResponse('');
  };

  const runTest = () => {
    // Simulate AI response
    setTestResponse('This is a simulated response from the AI model. In production, this would connect to your Ollama API.');
  };

  const saveAPIConfig = () => {
    setIsEditingAPI(false);
    alert('API configuration saved successfully!');
  };

  const saveModelConfig = () => {
    if (editingModel) {
      setModels(models.map(m => m.id === editingModel.id ? editingModel : m));
      setEditingModel(null);
    }
  };

  const addModel = () => {
    if (newModel.name && newModel.displayName && newModel.description) {
      const newId = (models.length + 1).toString();
      const completeNewModel: AIModel = {
        id: newId,
        name: newModel.name,
        displayName: newModel.displayName,
        description: newModel.description,
        status: newModel.status as 'active' | 'inactive',
        parameters: newModel.parameters!,
        stats: {
          totalRequests: 0,
          activeUsers: 0,
          avgResponseTime: 0
        },
        lastUsed: 'Never'
      };
      setModels([...models, completeNewModel]);
      setIsAddingModel(false);
      // Reset form
      setNewModel({
        name: '',
        displayName: '',
        description: '',
        status: 'active',
        parameters: {
          temperature: 0.7,
          maxTokens: 2048,
          topP: 0.9
        }
      });
    } else {
      alert('Please fill in all required fields (Name, Display Name, and Description).');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
            <MessageSquare className="size-8 text-blue-600" />
            Chat Management
          </h1>
          <p className="text-gray-600 mt-1">Manage Ollama AI models and API configuration</p>
        </div>
        <Button className="gap-2" onClick={() => setIsAddingModel(true)}>
          <Plus className="size-4" />
          Add New Model
        </Button>
      </div>

      {/* Stats Overview */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Total Requests</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">{totalRequests.toLocaleString()}</p>
              <p className="text-xs text-green-600 mt-1">↑ 12.5% from last week</p>
            </div>
            <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
              <MessageCircle className="size-6 text-blue-600" />
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Active Models</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">{activeModels}/{models.length}</p>
              <p className="text-xs text-gray-500 mt-1">Models in use</p>
            </div>
            <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
              <Zap className="size-6 text-green-600" />
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Active Users</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">{totalActiveUsers}</p>
              <p className="text-xs text-green-600 mt-1">↑ 8.3% increase</p>
            </div>
            <div className="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
              <Users className="size-6 text-purple-600" />
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">API Status</p>
              <p className="text-2xl font-bold text-gray-900 mt-1 flex items-center gap-2">
                <CheckCircle className="size-5 text-green-600" />
                Connected
              </p>
              <p className="text-xs text-gray-500 mt-1">{apiConfig.version}</p>
            </div>
            <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
              <Server className="size-6 text-green-600" />
            </div>
          </div>
        </Card>
      </div>

      {/* API Configuration */}
      <Card className="p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
              <Settings className="size-5 text-blue-600" />
              Ollama API Configuration
            </h3>
            <p className="text-sm text-gray-600 mt-1">Configure your Ollama API endpoint and settings</p>
          </div>
          <div className="flex gap-2">
            {isEditingAPI ? (
              <>
                <Button variant="outline" size="sm" onClick={() => setIsEditingAPI(false)}>
                  <X className="size-4 mr-2" />
                  Cancel
                </Button>
                <Button size="sm" onClick={saveAPIConfig}>
                  <Save className="size-4 mr-2" />
                  Save
                </Button>
              </>
            ) : (
              <Button variant="outline" size="sm" onClick={() => setIsEditingAPI(true)}>
                <Edit className="size-4 mr-2" />
                Edit
              </Button>
            )}
            <Button variant="outline" size="sm" onClick={testConnection}>
              <RefreshCw className="size-4 mr-2" />
              Test Connection
            </Button>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Endpoint URL</label>
            <input
              type="text"
              value={apiConfig.endpoint}
              onChange={(e) => setApiConfig({ ...apiConfig, endpoint: e.target.value })}
              disabled={!isEditingAPI}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Port</label>
            <input
              type="text"
              value={apiConfig.port}
              onChange={(e) => setApiConfig({ ...apiConfig, port: e.target.value })}
              disabled={!isEditingAPI}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Connection Status</label>
            <div className="flex items-center gap-2 px-4 py-2 bg-gray-50 rounded-lg">
              <div className={`w-2 h-2 rounded-full ${apiConfig.status === 'connected' ? 'bg-green-500' : 'bg-red-500'}`}></div>
              <span className="text-sm font-medium text-gray-900 capitalize">{apiConfig.status}</span>
            </div>
          </div>
        </div>
      </Card>

      {/* AI Models List */}
      <Card className="p-6">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
            <Activity className="size-5 text-blue-600" />
            AI Models
          </h3>
          <Badge variant="outline">{models.length} models configured</Badge>
        </div>

        <div className="space-y-4">
          {models.map((model) => (
            <div key={model.id} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h4 className="text-lg font-semibold text-gray-900">{model.displayName}</h4>
                    <Badge variant={model.status === 'active' ? 'default' : 'secondary'}>
                      {model.status === 'active' ? (
                        <CheckCircle className="size-3 mr-1" />
                      ) : (
                        <XCircle className="size-3 mr-1" />
                      )}
                      {model.status}
                    </Badge>
                    <span className="text-xs text-gray-500">Model: {model.name}</span>
                  </div>
                  <p className="text-sm text-gray-600 mb-3">{model.description}</p>

                  <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-3">
                    <div>
                      <p className="text-xs text-gray-500">Total Requests</p>
                      <p className="text-sm font-semibold text-gray-900">{model.stats.totalRequests.toLocaleString()}</p>
                    </div>
                    <div>
                      <p className="text-xs text-gray-500">Active Users</p>
                      <p className="text-sm font-semibold text-gray-900">{model.stats.activeUsers}</p>
                    </div>
                    <div>
                      <p className="text-xs text-gray-500">Avg Response</p>
                      <p className="text-sm font-semibold text-gray-900">{model.stats.avgResponseTime}s</p>
                    </div>
                    <div>
                      <p className="text-xs text-gray-500">Temperature</p>
                      <p className="text-sm font-semibold text-gray-900">{model.parameters.temperature}</p>
                    </div>
                    <div>
                      <p className="text-xs text-gray-500">Last Used</p>
                      <p className="text-sm font-semibold text-gray-900">{model.lastUsed}</p>
                    </div>
                  </div>
                </div>

                <div className="flex gap-2 ml-4">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleTestModel(model.id)}
                    aria-label="Test Model"
                  >
                    <TestTube className="size-4 mr-2" />
                    Test
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setEditingModel(model)}
                    aria-label="Edit Model"
                  >
                    <Edit className="size-4" />
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => toggleModelStatus(model.id)}
                    aria-label="Toggle Status"
                  >
                    {model.status === 'active' ? (
                      <Pause className="size-4" />
                    ) : (
                      <Play className="size-4" />
                    )}
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => deleteModel(model.id)}
                    aria-label="Delete Model"
                  >
                    <Trash2 className="size-4 text-red-600" />
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Edit Model Modal */}
      {editingModel && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold text-gray-900">Edit Model Configuration</h3>
              <Button variant="ghost" size="sm" onClick={() => setEditingModel(null)}>
                <X className="size-5" />
              </Button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Display Name</label>
                <input
                  type="text"
                  value={editingModel.displayName}
                  onChange={(e) => setEditingModel({ ...editingModel, displayName: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Model Name</label>
                <input
                  type="text"
                  value={editingModel.name}
                  onChange={(e) => setEditingModel({ ...editingModel, name: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Description</label>
                <textarea
                  value={editingModel.description}
                  onChange={(e) => setEditingModel({ ...editingModel, description: e.target.value })}
                  rows={3}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Temperature</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="2"
                    value={editingModel.parameters.temperature}
                    onChange={(e) => setEditingModel({
                      ...editingModel,
                      parameters: { ...editingModel.parameters, temperature: parseFloat(e.target.value) }
                    })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Max Tokens</label>
                  <input
                    type="number"
                    value={editingModel.parameters.maxTokens}
                    onChange={(e) => setEditingModel({
                      ...editingModel,
                      parameters: { ...editingModel.parameters, maxTokens: parseInt(e.target.value) }
                    })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Top P</label>
                  <input
                    type="number"
                    step="0.05"
                    min="0"
                    max="1"
                    value={editingModel.parameters.topP}
                    onChange={(e) => setEditingModel({
                      ...editingModel,
                      parameters: { ...editingModel.parameters, topP: parseFloat(e.target.value) }
                    })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 mt-6">
              <Button variant="outline" onClick={() => setEditingModel(null)}>
                Cancel
              </Button>
              <Button onClick={saveModelConfig}>
                <Save className="size-4 mr-2" />
                Save Changes
              </Button>
            </div>
          </Card>
        </div>
      )}

      {/* Test Model Modal */}
      {testingModel && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="p-6 max-w-3xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold text-gray-900">Test AI Model</h3>
              <Button variant="ghost" size="sm" onClick={() => setTestingModel(null)}>
                <X className="size-5" />
              </Button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Test Prompt</label>
                <textarea
                  value={testPrompt}
                  onChange={(e) => setTestPrompt(e.target.value)}
                  placeholder="Enter your test prompt here..."
                  rows={4}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <Button onClick={runTest} className="w-full">
                <Play className="size-4 mr-2" />
                Run Test
              </Button>

              {testResponse && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">AI Response</label>
                  <div className="p-4 bg-gray-50 border border-gray-200 rounded-lg">
                    <p className="text-sm text-gray-900">{testResponse}</p>
                  </div>
                </div>
              )}
            </div>
          </Card>
        </div>
      )}

      {/* Add New Model Modal */}
      {isAddingModel && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold text-gray-900">Add New AI Model</h3>
              <Button variant="ghost" size="sm" onClick={() => setIsAddingModel(false)}>
                <X className="size-5" />
              </Button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Display Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={newModel.displayName}
                  onChange={(e) => setNewModel({ ...newModel, displayName: e.target.value })}
                  placeholder="e.g., GPT-4, Llama 2, Mistral 7B"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Model Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={newModel.name}
                  onChange={(e) => setNewModel({ ...newModel, name: e.target.value })}
                  placeholder="e.g., gpt-4, llama2, mistral"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <p className="text-xs text-gray-500 mt-1">This should match the model name in Ollama</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Description <span className="text-red-500">*</span>
                </label>
                <textarea
                  value={newModel.description}
                  onChange={(e) => setNewModel({ ...newModel, description: e.target.value })}
                  placeholder="Brief description of this model's capabilities and use cases"
                  rows={3}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Status</label>
                <select
                  value={newModel.status}
                  onChange={(e) => setNewModel({ ...newModel, status: e.target.value as 'active' | 'inactive' })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="active">Active</option>
                  <option value="inactive">Inactive</option>
                </select>
              </div>

              <div className="border-t border-gray-200 pt-4">
                <h4 className="text-sm font-semibold text-gray-900 mb-3">Model Parameters</h4>
                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Temperature</label>
                    <input
                      type="number"
                      step="0.1"
                      min="0"
                      max="2"
                      value={newModel.parameters?.temperature}
                      onChange={(e) => setNewModel({
                        ...newModel,
                        parameters: { ...newModel.parameters!, temperature: parseFloat(e.target.value) }
                      })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                    <p className="text-xs text-gray-500 mt-1">0-2 (default: 0.7)</p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Max Tokens</label>
                    <input
                      type="number"
                      value={newModel.parameters?.maxTokens}
                      onChange={(e) => setNewModel({
                        ...newModel,
                        parameters: { ...newModel.parameters!, maxTokens: parseInt(e.target.value) }
                      })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                    <p className="text-xs text-gray-500 mt-1">Maximum response length</p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Top P</label>
                    <input
                      type="number"
                      step="0.05"
                      min="0"
                      max="1"
                      value={newModel.parameters?.topP}
                      onChange={(e) => setNewModel({
                        ...newModel,
                        parameters: { ...newModel.parameters!, topP: parseFloat(e.target.value) }
                      })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                    <p className="text-xs text-gray-500 mt-1">0-1 (default: 0.9)</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 mt-6">
              <Button variant="outline" onClick={() => setIsAddingModel(false)}>
                Cancel
              </Button>
              <Button onClick={addModel}>
                <Plus className="size-4 mr-2" />
                Add Model
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}