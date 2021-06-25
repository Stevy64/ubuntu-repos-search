package LinuxPackageSearch;

public class Package {
	public String name;
	public String description;
	public String version;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Package other = (Package) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	public String toString() {
		 return "{\"name\": " + "\"" + this.name + "\"" + ", \"version\": " + "\"" + this.version + "\"" + ", \"description\": " + "\"" + this.description + "}";
	}
	
	
}
